package com.tenisahora.modules.instructor.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.instructor.dto.EntrenadorRequest;
import com.tenisahora.modules.instructor.dto.EntrenadorResponse;
import com.tenisahora.modules.instructor.dto.EntrenadorUpdateRequest;
import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.CertificacionResponse;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.GestionDePersonas;
import com.tenisahora.shared.persona.MapeoDePersona;
import com.tenisahora.shared.util.Textos;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EntrenadorService extends GestionDePersonas<Entrenador, EntrenadorResponse> {

    private static final int MAX_TAMANIO_PAGINA = 50;

    private final MapeoDePersona mapeo;
    private final DirectorioDeCuentas directorio;
    private final CertificacionDeInstructores certificacionService;
    private final ValidacionDeCertificaciones validacion;
    private final AsignacionDeInstructores asignacion;
    private final Clock clock;

    public EntrenadorService(EntrenadorRepository repositorio, MapeoDePersona mapeo, DirectorioDeCuentas directorio,
                             PasswordEncoder passwordEncoder, ApplicationEventPublisher eventos, PdfService pdfService,
                             CertificacionDeInstructores certificacionService, ValidacionDeCertificaciones validacion,
                             AsignacionDeInstructores asignacion, Clock clock) {
        super(repositorio, mapeo, directorio, passwordEncoder, eventos, pdfService);
        this.mapeo = mapeo;
        this.directorio = directorio;
        this.certificacionService = certificacionService;
        this.validacion = validacion;
        this.asignacion = asignacion;
        this.clock = clock;
    }

    @Override
    protected Entrenador nueva() {
        Entrenador entrenador = new Entrenador();
        entrenador.setFechaContratacion(LocalDate.now(clock));
        return entrenador;
    }

    @Override
    protected EntrenadorResponse aRespuesta(Entrenador entrenador) {
        CertificacionResponse certificacion = entrenador.getCertificacion() == null ? null
                : CertificacionResponse.de(entrenador.getCertificacion(), LocalDate.now(clock));
        return EntrenadorResponse.de(mapeo.aRespuesta(entrenador), entrenador.getCertificacionDeportiva(), certificacion,
                entrenador.getEstadoValidacion(), entrenador.getFechaValidacion(), entrenador.getMotivoRechazo(),
                entrenador.getEspecialidad(), entrenador.getValorPorHora());
    }

    @Override
    protected String tituloDeFicha() {
        return "Ficha de entrenador";
    }

    @Override
    protected List<CampoFicha> camposPropios(Entrenador entrenador) {
        List<CampoFicha> campos = new ArrayList<>();
        campos.add(new CampoFicha("Certificación deportiva", entrenador.getCertificacionDeportiva()));
        campos.add(new CampoFicha("Especialidad", entrenador.getEspecialidad()));
        campos.add(new CampoFicha("Estado de la certificación", entrenador.getEstadoValidacion().name()));
        campos.add(new CampoFicha("Fecha de validación", formatear(entrenador.getFechaValidacion())));
        campos.add(new CampoFicha("Motivo de rechazo", entrenador.getMotivoRechazo()));
        Certificacion certificacion = entrenador.getCertificacion();
        if (certificacion != null) {
            campos.add(new CampoFicha("N° de certificación", certificacion.getNumeroCertificacion()));
            campos.add(new CampoFicha("Entidad emisora", certificacion.getEntidadEmisora()));
            campos.add(new CampoFicha("Vencimiento de la certificación", formatear(certificacion.getFechaVencimiento())));
        }
        return campos;
    }

    // Buscar (CU27) con los datos propios: PersonaResumenResponse no trae especialidad, certificación ni estado.
    @Transactional(readOnly = true)
    public PageResponse<EntrenadorResponse> buscar(String dni, String nombre, String especialidad,
                                                   EstadoValidacion estadoValidacion, Boolean activo, int pagina, int tamanio) {
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), MAX_TAMANIO_PAGINA),
                Sort.by("apellido", "nombre", "id"));

        Specification<Entrenador> filtro = filtroDe(dni, nombre);
        if (especialidad != null && !especialidad.isBlank()) {
            filtro = filtro.and((entrenador, consulta, cb) ->
                    cb.equal(cb.lower(entrenador.get("especialidad")), especialidad.trim().toLowerCase()));
        }
        if (estadoValidacion != null) {
            filtro = filtro.and((entrenador, consulta, cb) -> cb.equal(entrenador.get("estadoValidacion"), estadoValidacion));
        }
        if (activo != null) {
            filtro = filtro.and((entrenador, consulta, cb) -> cb.equal(entrenador.get("activo"), activo));
        }

        return PageResponse.de(repositorio().findAll(filtro, pageable), this::aRespuesta);
    }

    @Transactional
    public EntrenadorResponse registrar(EntrenadorRequest request, Actor actor) {
        exigirPermiso(actor);
        validacion.exigirCertificacionDeportiva(request.certificacionDeportiva());

        DatosAlta datos = new DatosAlta(request.nombre(), request.apellido(), request.dni(), request.fechaNacimiento(),
                request.calle(), request.numeroCalle(), request.codigoArea(), request.numeroTelefono(),
                request.email(), request.password());
        Long id = crear(datos, "persona " + actor.id()).id();

        Entrenador entrenador = obtenerEntrenador(id);
        completarDatosDeAlta(entrenador, request);
        log.info("Entrenador {} registrado por persona {}", id, actor.id());
        return aRespuesta(entrenador);
    }

    private void completarDatosDeAlta(Entrenador entrenador, EntrenadorRequest request) {
        certificacionService.registrar(entrenador, request.certificacion());
        entrenador.setCertificacionDeportiva(request.certificacionDeportiva());
        entrenador.setEspecialidad(request.especialidad());
        entrenador.setValorPorHora(request.valorPorHora());
    }

    @Transactional
    public EntrenadorResponse modificar(Long id, EntrenadorUpdateRequest request, Actor actor) {
        exigirPermiso(actor);
        Entrenador entrenador = obtenerEntrenador(id);
        asegurarQueNoChoca(entrenador, request);
        mapeo.aplicar(aPersonaUpdateRequest(request), entrenador);
        aplicarCambiosPropios(entrenador, request);
        Entrenador guardado = guardarEntrenador(entrenador);
        log.info("Entrenador {} modificado por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    private PersonaUpdateRequest aPersonaUpdateRequest(EntrenadorUpdateRequest r) {
        return new PersonaUpdateRequest(r.nombre(), r.apellido(), r.dni(), r.fechaNacimiento(),
                r.calle(), r.numeroCalle(), r.codigoArea(), r.numeroTelefono(), r.email());
    }

    private void asegurarQueNoChoca(Entrenador entrenador, EntrenadorUpdateRequest cambios) {
        if (cambios.dni() != null && !cambios.dni().equals(entrenador.getDni())) {
            directorio.asegurarDniLibre(cambios.dni());
        }
        if (cambios.email() != null && !Textos.normalizarEmail(cambios.email()).equalsIgnoreCase(entrenador.getEmail())) {
            directorio.asegurarEmailLibre(Textos.normalizarEmail(cambios.email()));
        }
    }


    private void aplicarCambiosPropios(Entrenador entrenador, EntrenadorUpdateRequest cambios) {
        if (cambios.certificacionDeportiva() != null) {
            entrenador.setCertificacionDeportiva(cambios.certificacionDeportiva());
        }
        if (cambios.certificacion() != null) {
            certificacionService.reemplazar(entrenador, cambios.certificacion());
        }
        if (cambios.especialidad() != null) {
            entrenador.setEspecialidad(cambios.especialidad());
        }
        if (cambios.valorPorHora() != null) {
            entrenador.setValorPorHora(cambios.valorPorHora());
        }
    }

    @Transactional
    public EntrenadorResponse reemplazarCertificacion(Long id, CertificacionRequest request, Actor actor) {
        exigirPermiso(actor);
        Entrenador entrenador = obtenerEntrenador(id);
        certificacionService.reemplazar(entrenador, request);
        Entrenador guardado = guardarEntrenador(entrenador);
        log.info("Certificación del entrenador {} reemplazada por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    @Transactional
    public EntrenadorResponse decidirCertificacion(Long id, ValidacionRequest decision, Actor actor) {
        Entrenador entrenador = obtenerEntrenador(id);
        certificacionService.decidir(entrenador, decision, actor);
        Entrenador guardado = guardarEntrenador(entrenador);
        log.info("Certificación del entrenador {} decidida por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public HabilitacionResponse habilitacion(Long id) {
        return asignacion.habilitacion(Rol.ENTRENADOR, id);
    }

    @Transactional(readOnly = true)
    public PageResponse<PersonaResumenResponse> asignables(int pagina, int tamanio) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), MAX_TAMANIO_PAGINA),
                Sort.by("apellido", "nombre", "id"));
        return PageResponse.de(repositorio().findAll(sonAsignables(LocalDate.now(clock)), pageable), mapeo::aResumen);
    }

    /** Mismo criterio que InstructorProxy (RD03): validado y con la certificación vigente. */
    private Specification<Entrenador> sonAsignables(LocalDate hoy) {
        return (root, consulta, cb) -> {
            Join<Entrenador, Certificacion> certificacion = root.join("certificacion", JoinType.LEFT);
            return cb.and(
                    cb.equal(root.<EstadoValidacion>get("estadoValidacion"), EstadoValidacion.VALIDADO),
                    cb.or(cb.isNull(certificacion.get("fechaVencimiento")),
                            cb.greaterThanOrEqualTo(certificacion.<LocalDate>get("fechaVencimiento"), hoy)));
        };
    }

    private Entrenador obtenerEntrenador(Long id) {
        return repositorio().findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));
    }

    private Entrenador guardarEntrenador(Entrenador entrenador) {
        try {
            return repositorio().saveAndFlush(entrenador);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("dni") || msg.contains("email") || msg.contains("uk") || msg.contains("unique")) {
                throw new ConflictoException(CodigoError.PERSONA_DUPLICADA, "Ya existe una persona con ese DNI o correo electrónico");
            }
            throw e;
        }
    }
}
