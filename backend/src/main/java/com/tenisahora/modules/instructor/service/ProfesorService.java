package com.tenisahora.modules.instructor.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.CertificacionResponse;
import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.dto.ProfesorRequest;
import com.tenisahora.modules.instructor.dto.ProfesorResponse;
import com.tenisahora.modules.instructor.dto.ProfesorUpdateRequest;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.GestionDePersonas;
import com.tenisahora.shared.persona.MapeoDePersona;
import com.tenisahora.shared.util.Textos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** ABM del profesor (CU20-CU24), su certificación (CU30) y su validación (CU31), sobre GestionDePersonas. */
@Slf4j
@Service
public class ProfesorService extends GestionDePersonas<Profesor, ProfesorResponse> {

    private final MapeoDePersona mapeo;
    private final DirectorioDeCuentas directorio;
    private final CertificacionDeInstructores certificacionService;
    private final ValidacionDeCertificaciones validacion;
    private final AsignacionDeInstructores asignacion;
    private final Clock clock;

    public ProfesorService(ProfesorRepository repositorio, MapeoDePersona mapeo, DirectorioDeCuentas directorio,
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
    protected Profesor nueva() {
        Profesor profesor = new Profesor();
        profesor.setFechaContratacion(LocalDate.now(clock));
        return profesor;
    }

    @Override
    protected ProfesorResponse aRespuesta(Profesor profesor) {
        CertificacionResponse certificacion = profesor.getCertificacion() == null ? null
                : CertificacionResponse.de(profesor.getCertificacion(), LocalDate.now(clock));
        return ProfesorResponse.de(mapeo.aRespuesta(profesor), profesor.getTituloHabilitante(), certificacion,
                profesor.getEstadoValidacion(), profesor.getFechaValidacion(), profesor.getEspecialidad(), profesor.getValorPorHora(),
                profesor.getPermisoPersonalAutorizado());
    }

    @Override
    protected String tituloDeFicha() {
        return "Ficha de profesor";
    }


    @Override
    protected List<CampoFicha> camposPropios(Profesor profesor) {
        List<CampoFicha> campos = new ArrayList<>();
        campos.add(new CampoFicha("Título habilitante", profesor.getTituloHabilitante()));
        campos.add(new CampoFicha("Especialidad", profesor.getEspecialidad()));
        campos.add(new CampoFicha("Estado del título", profesor.getEstadoValidacion().name()));
        campos.add(new CampoFicha("Fecha de validación", formatear(profesor.getFechaValidacion())));
        campos.add(new CampoFicha("Motivo de rechazo", profesor.getMotivoRechazo()));
        Certificacion certificacion = profesor.getCertificacion();
        if (certificacion != null) {
            campos.add(new CampoFicha("N° de certificación", certificacion.getNumeroCertificacion()));
            campos.add(new CampoFicha("Entidad emisora", certificacion.getEntidadEmisora()));
            campos.add(new CampoFicha("Vencimiento del título", formatear(certificacion.getFechaVencimiento())));
        }
        return campos;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfesorResponse> buscar(String dni, String nombre, EstadoValidacion estadoValidacion,
                                                 Boolean activo, int pagina, int tamanio) {
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), 50),
                Sort.by("apellido", "nombre", "id"));

        Specification<Profesor> filtro = filtroDe(dni, nombre);
        if (estadoValidacion != null) {
            filtro = filtro.and((profesor, consulta, cb) -> cb.equal(profesor.get("estadoValidacion"), estadoValidacion));
        }
        if (activo != null) {
            filtro = filtro.and((profesor, consulta, cb) -> cb.equal(profesor.get("activo"), activo));
        }

        return PageResponse.de(repositorio().findAll(filtro, pageable), this::aRespuesta);
    }

    @Transactional
    public ProfesorResponse registrar(ProfesorRequest request, Actor actor) {
        exigirPermiso(actor);
        validacion.exigirTitulo(request.tituloHabilitante());

        DatosAlta datos = new DatosAlta(request.nombre(), request.apellido(), request.dni(), request.fechaNacimiento(),
                request.calle(), request.numeroCalle(), request.codigoArea(), request.numeroTelefono(),
                request.email(), request.password());
        Long id = crear(datos, "persona " + actor.id()).id();

        Profesor profesor = obtenerProfesor(id);
        completarDatosDeAlta(profesor, request);
        log.info("Profesor {} registrado por persona {}", id, actor.id());
        return aRespuesta(profesor);
    }

    private void completarDatosDeAlta(Profesor profesor, ProfesorRequest request) {
        certificacionService.registrar(profesor, request.certificacion());
        profesor.setTituloHabilitante(request.tituloHabilitante());
        profesor.setEspecialidad(request.especialidad());
        profesor.setValorPorHora(request.valorPorHora());
        profesor.setPermisoPersonalAutorizado(Boolean.TRUE.equals(request.permisoPersonalAutorizado()));
    }

    @Transactional
    public ProfesorResponse modificar(Long id, ProfesorUpdateRequest request, Actor actor) {
        exigirPermiso(actor);
        Profesor profesor = obtenerProfesor(id);
        asegurarQueNoChoca(profesor, request);
        mapeo.aplicar(aPersonaUpdateRequest(request), profesor);
        aplicarCambiosPropios(profesor, request);
        Profesor guardado = guardarProfesor(profesor);
        log.info("Profesor {} modificado por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    private PersonaUpdateRequest aPersonaUpdateRequest(ProfesorUpdateRequest r) {
        return new PersonaUpdateRequest(r.nombre(), r.apellido(), r.dni(), r.fechaNacimiento(),
                r.calle(), r.numeroCalle(), r.codigoArea(), r.numeroTelefono(), r.email());
    }

    
    private void asegurarQueNoChoca(Profesor profesor, ProfesorUpdateRequest cambios) {
        if (cambios.dni() != null && !cambios.dni().equals(profesor.getDni())) {
            directorio.asegurarDniLibre(cambios.dni());
        }
        if (cambios.email() != null && !Textos.normalizarEmail(cambios.email()).equalsIgnoreCase(profesor.getEmail())) {
            directorio.asegurarEmailLibre(Textos.normalizarEmail(cambios.email()));
        }
    }

    
    private void aplicarCambiosPropios(Profesor profesor, ProfesorUpdateRequest cambios) {
        boolean cambioTitulo = cambios.tituloHabilitante() != null
                && !cambios.tituloHabilitante().equals(profesor.getTituloHabilitante());
        if (cambioTitulo) {
            profesor.setTituloHabilitante(cambios.tituloHabilitante());
        }
        if (cambios.certificacion() != null) {
            certificacionService.reemplazar(profesor, cambios.certificacion());
        } else if (cambioTitulo) {
            certificacionService.reiniciarValidacion(profesor);
        }
        if (cambios.especialidad() != null) {
            profesor.setEspecialidad(cambios.especialidad());
        }
        if (cambios.valorPorHora() != null) {
            profesor.setValorPorHora(cambios.valorPorHora());
        }
        if (cambios.permisoPersonalAutorizado() != null) {
            profesor.setPermisoPersonalAutorizado(cambios.permisoPersonalAutorizado());
        }
    }

    

    @Transactional
    public ProfesorResponse reemplazarCertificacion(Long id, CertificacionRequest request, Actor actor) {
        exigirPermiso(actor);
        Profesor profesor = obtenerProfesor(id);
        certificacionService.reemplazar(profesor, request);
        Profesor guardado = guardarProfesor(profesor);
        log.info("Certificación del profesor {} reemplazada por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    @Transactional
    public ProfesorResponse decidirCertificacion(Long id, ValidacionRequest decision, Actor actor) {
        Profesor profesor = obtenerProfesor(id);
        certificacionService.decidir(profesor, decision, actor);
        Profesor guardado = guardarProfesor(profesor);
        log.info("Certificación del profesor {} decidida por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public HabilitacionResponse habilitacion(Long id) {
        return asignacion.habilitacion(Rol.PROFESOR, id);
    }

    private Profesor obtenerProfesor(Long id) {
        return repositorio().findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));
    }

    private Profesor guardarProfesor(Profesor profesor) {
        try {
            return repositorio().saveAndFlush(profesor);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException(CodigoError.PERSONA_DUPLICADA, "Ya existe una persona con ese DNI o correo electrónico");
        }
    }
}
