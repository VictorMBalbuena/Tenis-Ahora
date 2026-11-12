package com.tenisahora.modules.actividad.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.actividad.dto.ClaseRequest;
import com.tenisahora.modules.actividad.dto.ClaseResponse;
import com.tenisahora.modules.actividad.dto.ClaseUpdateRequest;
import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.actividad.repository.ClaseRepository;
import com.tenisahora.modules.actividad.repository.ClaseSpecs;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaseServiceImpl implements ClaseService {

    private static final Sort ORDEN = Sort.by(Sort.Order.asc("fechaInicio"), Sort.Order.asc("horaInicio"));

    private final ClaseRepository claseRepository;
    private final ActividadRepository actividadRepository;
    private final ActividadDiaRepository diasRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final CanchaRepository canchaRepository;
    private final ProfesorRepository profesorRepository;
    private final ValidadorDeAgendaDeActividades validador;
    private final CupoDeActividad cupoDeClase;
    private final ArmadoDeRespuestasDeClase respuestas;
    private final PdfService pdfService;

    // ---------- Alta (CU32) ----------

    @Override
    @Transactional
    public ClaseResponse programar(ClaseRequest request, Actor actor) {
        Profesor profesor = profesorValidado(request.profesorId());
        Cancha cancha = canchaExistente(request.canchaId());
        FranjaHoraria franja = FranjaHoraria.sinRestriccionDeMinuto(request.horaInicio(), request.horaFin(), CodigoError.HORARIO_INVALIDO);
        asegurarRangoDeFechas(request.fechaInicio(), request.fechaFin());
        validador.asegurarAgendaLibre(cancha, request.fechaInicio(), request.fechaFin(), request.dias(), franja, null);
        int cupo = cupoDeClase.asignar(request.cupoMaximo());

        Clase clase = nueva(cancha, profesor, request, franja, cupo);
        claseRepository.saveAndFlush(clase);
        clase.setCodigo(ClaseMapper.codigoDe(clase.getId()));
        claseRepository.saveAndFlush(clase);
        guardarDias(clase, request.dias());
        log.info("Clase {} programada por la persona {} con el profesor {}", clase.getId(), actor.id(), profesor.getId());
        return respuestas.deClase(clase);
    }

    // ---------- Consultas (CU34) ----------

    @Override
    @Transactional(readOnly = true)
    public ClaseResponse consultar(Long id) {
        return respuestas.deClase(claseExistente(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClaseResponse> buscar(Long canchaId, Long profesorId, LocalDate fecha, DayOfWeek dia, NivelClase nivel,
                                              EstadoActividad estado, int page, int size) {
        List<Specification<Clase>> filtros = filtrosDe(canchaId, profesorId, fecha, dia, nivel, estado);
        Page<Clase> pagina = claseRepository.findAll(Specification.allOf(filtros), PageRequest.of(page, size, ORDEN));
        return new PageResponse<>(respuestas.deClases(pagina.getContent()), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    // ---------- Modificación (CU33) ----------

    @Override
    @Transactional
    public ClaseResponse modificar(Long id, ClaseUpdateRequest request, Actor actor) {
        Clase clase = claseParaModificar(id);
        Cancha cancha = request.canchaId() != null ? canchaExistente(request.canchaId()) : clase.getCancha();
        Profesor profesor = request.profesorId() != null ? profesorValidado(request.profesorId()) : clase.getProfesor();
        LocalDate fechaInicio = valor(request.fechaInicio(), clase.getFechaInicio());
        LocalDate fechaFin = valor(request.fechaFin(), clase.getFechaFin());
        asegurarRangoDeFechas(fechaInicio, fechaFin);
        FranjaHoraria franja = FranjaHoraria.sinRestriccionDeMinuto(valor(request.horaInicio(), clase.getHoraInicio()),
                valor(request.horaFin(), clase.getHoraFin()), CodigoError.HORARIO_INVALIDO);
        Set<DayOfWeek> dias = request.dias() != null ? request.dias() : diasDe(clase.getId());
        validador.asegurarAgendaLibre(cancha, fechaInicio, fechaFin, dias, franja, clase.getId());
        int cupo = cupoNuevo(clase, request);

        aplicarCambios(clase, cancha, profesor, fechaInicio, fechaFin, franja, cupo, request);
        claseRepository.saveAndFlush(clase);
        actualizarDiasSiCambiaron(clase, request.dias());
        log.info("Clase {} modificada por la persona {}", id, actor.id());
        return respuestas.deClase(clase);
    }

    // ---------- Baja (CU35) ----------

    @Override
    @Transactional
    public void eliminar(Long id) {
        Clase clase = claseExistente(id);
        if (inscripcionRepository.existsByActividadIdAndActivaTrue(id)) {
            throw new ConflictoException(CodigoError.CLASE_CON_INSCRIPTOS, "No se puede eliminar: cancelá las inscripciones primero");
        }
        diasRepository.deleteByActividadId(id);
        claseRepository.delete(clase);
    }

    // ---------- Ficha (CU36) ----------

    @Override
    @Transactional(readOnly = true)
    public byte[] imprimir(Long id) {
        Clase clase = claseExistente(id);
        return pdfService.generarFicha("Ficha de clase", camposDeFicha(clase));
    }

    // ---------- Auxiliares ----------

    private List<CampoFicha> camposDeFicha(Clase clase) {
        int ocupado = cupoDeClase.ocupado(clase.getId());
        return List.of(
                new CampoFicha("Código", clase.getCodigo()),
                new CampoFicha("Nombre", clase.getNombre()),
                new CampoFicha("Nivel", String.valueOf(clase.getNivel())),
                new CampoFicha("Cancha", clase.getCancha().getIdentificador()),
                new CampoFicha("Profesor", clase.getProfesor().getApellido() + ", " + clase.getProfesor().getNombre()),
                new CampoFicha("Horario", clase.getHoraInicio() + " a " + clase.getHoraFin()),
                new CampoFicha("Vigencia", clase.getFechaInicio() + " a " + clase.getFechaFin()),
                new CampoFicha("Cupo", ocupado + " / " + clase.getCupoMaximo()));
    }

    private List<Specification<Clase>> filtrosDe(Long canchaId, Long profesorId, LocalDate fecha, DayOfWeek dia,
                                                  NivelClase nivel, EstadoActividad estado) {
        List<Specification<Clase>> filtros = new ArrayList<>();
        if (canchaId != null) {
            filtros.add(ClaseSpecs.deCancha(canchaId));
        }
        if (profesorId != null) {
            filtros.add(ClaseSpecs.deProfesor(profesorId));
        }
        if (fecha != null) {
            filtros.add(ClaseSpecs.enFecha(fecha));
        }
        if (dia != null) {
            filtros.add(ClaseSpecs.enDia(dia));
        }
        if (nivel != null) {
            filtros.add(ClaseSpecs.conNivel(nivel));
        }
        if (estado != null) {
            filtros.add(ClaseSpecs.conEstado(estado));
        }
        return filtros;
    }

    private int cupoNuevo(Clase clase, ClaseUpdateRequest request) {
        if (request.cupoMaximo() == null) {
            return clase.getCupoMaximo();
        }
        int cupo = cupoDeClase.asignar(request.cupoMaximo());
        cupoDeClase.asegurarCupoSuficienteParaModificar(clase.getId(), cupo);
        return cupo;
    }

    private void aplicarCambios(Clase clase, Cancha cancha, Profesor profesor, LocalDate fechaInicio, LocalDate fechaFin,
                                FranjaHoraria franja, int cupo, ClaseUpdateRequest request) {
        clase.setCancha(cancha);
        clase.setProfesor(profesor);
        clase.setFechaInicio(fechaInicio);
        clase.setFechaFin(fechaFin);
        clase.setHoraInicio(franja.desde());
        clase.setHoraFin(franja.hasta());
        clase.setCupoMaximo(cupo);
        clase.setNombre(valor(request.nombre(), clase.getNombre()));
        clase.setModalidad(valor(request.modalidad(), clase.getModalidad()));
        clase.setNivel(valor(request.nivel(), clase.getNivel()));
        clase.setCupoMinimo(valor(request.cupoMinimo(), clase.getCupoMinimo()));
        clase.setValorMensual(valor(request.valorMensual(), clase.getValorMensual()));
        clase.setCategoria(valor(request.categoria(), clase.getCategoria()));
    }

    private void actualizarDiasSiCambiaron(Clase clase, Set<DayOfWeek> dias) {
        if (dias == null) {
            return;
        }
        diasRepository.deleteByActividadId(clase.getId());
        guardarDias(clase, dias);
    }

    private Clase nueva(Cancha cancha, Profesor profesor, ClaseRequest request, FranjaHoraria franja, int cupo) {
        Clase clase = new Clase();
        clase.setNombre(request.nombre());
        clase.setModalidad(request.modalidad());
        clase.setNivel(request.nivel());
        clase.setCancha(cancha);
        clase.setProfesor(profesor);
        clase.setFechaInicio(request.fechaInicio());
        clase.setFechaFin(request.fechaFin());
        clase.setHoraInicio(franja.desde());
        clase.setHoraFin(franja.hasta());
        clase.setEstado(EstadoActividad.PROGRAMADA);
        clase.setCupoMaximo(cupo);
        clase.setCupoMinimo(request.cupoMinimo());
        clase.setValorMensual(request.valorMensual());
        clase.setCategoria(request.categoria());
        return clase;
    }

    private void guardarDias(Clase clase, Set<DayOfWeek> dias) {
        diasRepository.saveAll(dias.stream().map(dia -> new ActividadDia(clase, dia)).toList());
    }

    private Set<DayOfWeek> diasDe(Long actividadId) {
        return diasRepository.findByActividadId(actividadId).stream().map(ActividadDia::getDiaSemana).collect(Collectors.toSet());
    }

    private void asegurarRangoDeFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new SolicitudInvalidaException(CodigoError.VALIDACION_FALLIDA, "La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private Profesor profesorValidado(Long profesorId) {
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Profesor no encontrado"));
        if (!profesor.estaValidado()) {
            throw new ReglaDeNegocioException(CodigoError.PROFESOR_NO_VALIDADO, "El profesor no tiene la certificación validada");
        }
        return profesor;
    }

    private Cancha canchaExistente(Long canchaId) {
        return canchaRepository.findById(canchaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));
    }

    private Clase claseExistente(Long id) {
        return claseRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CLASE_NO_ENCONTRADA, "Clase no encontrada"));
    }

    /** Bloquea la actividad para modificar (lo necesita el cupo) y comprueba que sea una clase todavía programada. */
    private Clase claseParaModificar(Long id) {
        Actividad actividad = actividadRepository.buscarParaModificar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CLASE_NO_ENCONTRADA, "Clase no encontrada"));
        if (!(actividad instanceof Clase clase)) {
            throw new RecursoNoEncontradoException(CodigoError.CLASE_NO_ENCONTRADA, "Clase no encontrada");
        }
        if (!clase.getEstado().puedeModificarse()) {
            throw new ReglaDeNegocioException(CodigoError.CLASE_NO_MODIFICABLE,
                    "La clase está " + clase.getEstado() + ": ya no se puede modificar");
        }
        return clase;
    }

    private static <T> T valor(T nuevo, T actual) {
        return nuevo != null ? nuevo : actual;
    }
}
