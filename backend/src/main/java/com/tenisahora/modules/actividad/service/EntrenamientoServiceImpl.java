package com.tenisahora.modules.actividad.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.actividad.dto.EntrenamientoRequest;
import com.tenisahora.modules.actividad.dto.EntrenamientoResponse;
import com.tenisahora.modules.actividad.dto.EntrenamientoUpdateRequest;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.actividad.repository.EntrenamientoRepository;
import com.tenisahora.modules.actividad.repository.EntrenamientoSpecs;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Espejo de ClaseServiceImpl, apoyado en los mismos colaboradores generalizados en la Fase 2 de BACK 12. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntrenamientoServiceImpl implements EntrenamientoService {

    private static final Sort ORDEN = Sort.by(Sort.Order.asc("fechaInicio"), Sort.Order.asc("horaInicio"));

    private final EntrenamientoRepository entrenamientoRepository;
    private final ActividadRepository actividadRepository;
    private final ActividadDiaRepository diasRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final CanchaRepository canchaRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final ValidadorDeAgendaDeActividades validador;
    private final CupoDeActividad cupoDeActividad;
    private final ValidacionDeInstructorPorModalidad validacionDeInstructor;
    private final ArmadoDeRespuestasDeEntrenamiento respuestas;
    private final PdfService pdfService;

    // ---------- Alta (CU37) ----------

    @Override
    @Transactional
    public EntrenamientoResponse programar(EntrenamientoRequest request, Actor actor) {
        Entrenador entrenador = entrenadorValidado(request.entrenadorId());
        Cancha cancha = canchaExistente(request.canchaId());
        FranjaHoraria franja = FranjaHoraria.sinRestriccionDeMinuto(request.horaInicio(), request.horaFin(), CodigoError.HORARIO_INVALIDO);
        asegurarRangoDeFechas(request.fechaInicio(), request.fechaFin());
        validador.asegurarAgendaLibre(cancha, request.fechaInicio(), request.fechaFin(), request.dias(), franja, null);
        int cupo = cupoDeActividad.asignar(request.cupoMaximo());

        Entrenamiento entrenamiento = nuevo(cancha, entrenador, request, franja, cupo);
        validacionDeInstructor.asegurarValida(entrenamiento);
        entrenamientoRepository.saveAndFlush(entrenamiento);
        entrenamiento.setCodigo(ClaseMapper.codigoDe(entrenamiento.getId()));
        entrenamientoRepository.saveAndFlush(entrenamiento);
        guardarDias(entrenamiento, request.dias());
        log.info("Entrenamiento {} programado por la persona {} con el entrenador {}", entrenamiento.getId(), actor.id(), entrenador.getId());
        return respuestas.deEntrenamiento(entrenamiento);
    }

    // ---------- Consultas (CU39) ----------

    @Override
    @Transactional(readOnly = true)
    public EntrenamientoResponse consultar(Long id) {
        return respuestas.deEntrenamiento(entrenamientoExistente(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EntrenamientoResponse> buscar(Long canchaId, Long entrenadorId, LocalDate fecha, DayOfWeek dia,
                                                       EstadoActividad estado, int page, int size) {
        List<Specification<Entrenamiento>> filtros = filtrosDe(canchaId, entrenadorId, fecha, dia, estado);
        Page<Entrenamiento> pagina = entrenamientoRepository.findAll(Specification.allOf(filtros), PageRequest.of(page, size, ORDEN));
        return new PageResponse<>(respuestas.deEntrenamientos(pagina.getContent()), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    // ---------- Modificación (CU38) ----------

    @Override
    @Transactional
    public EntrenamientoResponse modificar(Long id, EntrenamientoUpdateRequest request, Actor actor) {
        Entrenamiento entrenamiento = entrenamientoParaModificar(id);
        Cancha cancha = request.canchaId() != null ? canchaExistente(request.canchaId()) : entrenamiento.getCancha();
        Entrenador entrenador = request.entrenadorId() != null ? entrenadorValidado(request.entrenadorId()) : entrenamiento.getEntrenador();
        LocalDate fechaInicio = valor(request.fechaInicio(), entrenamiento.getFechaInicio());
        LocalDate fechaFin = valor(request.fechaFin(), entrenamiento.getFechaFin());
        asegurarRangoDeFechas(fechaInicio, fechaFin);
        FranjaHoraria franja = FranjaHoraria.sinRestriccionDeMinuto(valor(request.horaInicio(), entrenamiento.getHoraInicio()),
                valor(request.horaFin(), entrenamiento.getHoraFin()), CodigoError.HORARIO_INVALIDO);
        Set<DayOfWeek> dias = request.dias() != null ? request.dias() : diasDe(entrenamiento.getId());
        validador.asegurarAgendaLibre(cancha, fechaInicio, fechaFin, dias, franja, entrenamiento.getId());
        int cupo = cupoNuevo(entrenamiento, request);

        aplicarCambios(entrenamiento, cancha, entrenador, fechaInicio, fechaFin, franja, cupo, request);
        validacionDeInstructor.asegurarValida(entrenamiento);
        entrenamientoRepository.saveAndFlush(entrenamiento);
        actualizarDiasSiCambiaron(entrenamiento, request.dias());
        log.info("Entrenamiento {} modificado por la persona {}", id, actor.id());
        return respuestas.deEntrenamiento(entrenamiento);
    }

    // ---------- Baja (CU40) ----------

    @Override
    @Transactional
    public void eliminar(Long id) {
        Entrenamiento entrenamiento = entrenamientoExistente(id);
        if (inscripcionRepository.existsByActividadIdAndActivaTrue(id)) {
            throw new ConflictoException(CodigoError.ACTIVIDAD_CON_INSCRIPTOS, "No se puede eliminar: cancelá las inscripciones primero");
        }
        diasRepository.deleteByActividadId(id);
        entrenamientoRepository.delete(entrenamiento);
    }

    // ---------- Ficha (CU41) ----------

    @Override
    @Transactional(readOnly = true)
    public byte[] imprimir(Long id) {
        Entrenamiento entrenamiento = entrenamientoExistente(id);
        return pdfService.generarFicha("Ficha de entrenamiento", camposDeFicha(entrenamiento));
    }

    // ---------- Auxiliares ----------

    private List<CampoFicha> camposDeFicha(Entrenamiento entrenamiento) {
        int ocupado = cupoDeActividad.ocupado(entrenamiento.getId());
        return List.of(
                new CampoFicha("Código", entrenamiento.getCodigo()),
                new CampoFicha("Nombre", entrenamiento.getNombre()),
                new CampoFicha("Objetivo", entrenamiento.getObjetivo()),
                new CampoFicha("Cancha", entrenamiento.getCancha().getIdentificador()),
                new CampoFicha("Entrenador", entrenamiento.getEntrenador().getApellido() + ", " + entrenamiento.getEntrenador().getNombre()),
                new CampoFicha("Horario", entrenamiento.getHoraInicio() + " a " + entrenamiento.getHoraFin()),
                new CampoFicha("Vigencia", entrenamiento.getFechaInicio() + " a " + entrenamiento.getFechaFin()),
                new CampoFicha("Cupo", ocupado + " / " + entrenamiento.getCupoMaximo()));
    }

    private List<Specification<Entrenamiento>> filtrosDe(Long canchaId, Long entrenadorId, LocalDate fecha, DayOfWeek dia,
                                                          EstadoActividad estado) {
        List<Specification<Entrenamiento>> filtros = new ArrayList<>();
        if (canchaId != null) {
            filtros.add(EntrenamientoSpecs.deCancha(canchaId));
        }
        if (entrenadorId != null) {
            filtros.add(EntrenamientoSpecs.deEntrenador(entrenadorId));
        }
        if (fecha != null) {
            filtros.add(EntrenamientoSpecs.enFecha(fecha));
        }
        if (dia != null) {
            filtros.add(EntrenamientoSpecs.enDia(dia));
        }
        if (estado != null) {
            filtros.add(EntrenamientoSpecs.conEstado(estado));
        }
        return filtros;
    }

    private int cupoNuevo(Entrenamiento entrenamiento, EntrenamientoUpdateRequest request) {
        if (request.cupoMaximo() == null) {
            return entrenamiento.getCupoMaximo();
        }
        int cupo = cupoDeActividad.asignar(request.cupoMaximo());
        cupoDeActividad.asegurarCupoSuficienteParaModificar(entrenamiento.getId(), cupo);
        return cupo;
    }

    private void aplicarCambios(Entrenamiento entrenamiento, Cancha cancha, Entrenador entrenador, LocalDate fechaInicio, LocalDate fechaFin,
                                FranjaHoraria franja, int cupo, EntrenamientoUpdateRequest request) {
        entrenamiento.setCancha(cancha);
        entrenamiento.setEntrenador(entrenador);
        entrenamiento.setFechaInicio(fechaInicio);
        entrenamiento.setFechaFin(fechaFin);
        entrenamiento.setHoraInicio(franja.desde());
        entrenamiento.setHoraFin(franja.hasta());
        entrenamiento.setCupoMaximo(cupo);
        entrenamiento.setNombre(valor(request.nombre(), entrenamiento.getNombre()));
        entrenamiento.setModalidad(valor(request.modalidad(), entrenamiento.getModalidad()));
        entrenamiento.setObjetivo(valor(request.objetivo(), entrenamiento.getObjetivo()));
        entrenamiento.setCupoMinimo(valor(request.cupoMinimo(), entrenamiento.getCupoMinimo()));
        entrenamiento.setValorMensual(valor(request.valorMensual(), entrenamiento.getValorMensual()));
        entrenamiento.setCategoria(valor(request.categoria(), entrenamiento.getCategoria()));
    }

    private void actualizarDiasSiCambiaron(Entrenamiento entrenamiento, Set<DayOfWeek> dias) {
        if (dias == null) {
            return;
        }
        diasRepository.deleteByActividadId(entrenamiento.getId());
        guardarDias(entrenamiento, dias);
    }

    private Entrenamiento nuevo(Cancha cancha, Entrenador entrenador, EntrenamientoRequest request, FranjaHoraria franja, int cupo) {
        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setNombre(request.nombre());
        entrenamiento.setModalidad(request.modalidad());
        entrenamiento.setObjetivo(request.objetivo());
        entrenamiento.setCancha(cancha);
        entrenamiento.setEntrenador(entrenador);
        entrenamiento.setFechaInicio(request.fechaInicio());
        entrenamiento.setFechaFin(request.fechaFin());
        entrenamiento.setHoraInicio(franja.desde());
        entrenamiento.setHoraFin(franja.hasta());
        entrenamiento.setEstado(EstadoActividad.PROGRAMADA);
        entrenamiento.setCupoMaximo(cupo);
        entrenamiento.setCupoMinimo(request.cupoMinimo());
        entrenamiento.setValorMensual(request.valorMensual());
        entrenamiento.setCategoria(request.categoria());
        return entrenamiento;
    }

    private void guardarDias(Entrenamiento entrenamiento, Set<DayOfWeek> dias) {
        diasRepository.saveAll(dias.stream().map(dia -> new ActividadDia(entrenamiento, dia)).toList());
    }

    private Set<DayOfWeek> diasDe(Long actividadId) {
        return diasRepository.findByActividadId(actividadId).stream().map(ActividadDia::getDiaSemana).collect(Collectors.toSet());
    }

    private void asegurarRangoDeFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new SolicitudInvalidaException(CodigoError.VALIDACION_FALLIDA, "La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private Entrenador entrenadorValidado(Long entrenadorId) {
        Entrenador entrenador = entrenadorRepository.findById(entrenadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Entrenador no encontrado"));
        if (!entrenador.estaValidado()) {
            throw new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "El entrenador no tiene la certificación validada");
        }
        return entrenador;
    }

    private Cancha canchaExistente(Long canchaId) {
        return canchaRepository.findById(canchaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));
    }

    private Entrenamiento entrenamientoExistente(Long id) {
        return entrenamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, "Entrenamiento no encontrado"));
    }

    /** Bloquea la actividad para modificar (lo necesita el cupo) y comprueba que sea un entrenamiento todavía programado. */
    private Entrenamiento entrenamientoParaModificar(Long id) {
        Actividad actividad = actividadRepository.buscarParaModificar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, "Entrenamiento no encontrado"));
        if (!(actividad instanceof Entrenamiento entrenamiento)) {
            throw new RecursoNoEncontradoException(CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, "Entrenamiento no encontrado");
        }
        if (!entrenamiento.getEstado().puedeModificarse()) {
            throw new ReglaDeNegocioException(CodigoError.ACTIVIDAD_NO_MODIFICABLE,
                    "El entrenamiento está " + entrenamiento.getEstado() + ": ya no se puede modificar");
        }
        return entrenamiento;
    }

    private static <T> T valor(T nuevo, T actual) {
        return nuevo != null ? nuevo : actual;
    }
}
