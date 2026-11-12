package com.tenisahora.modules.instructor.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.ProfesorRequest;
import com.tenisahora.modules.instructor.dto.ProfesorResponse;
import com.tenisahora.modules.instructor.dto.ProfesorUpdateRequest;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.MapeoDePersona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfesorServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private ProfesorRepository repositorio;
    @Mock
    private CertificacionRepository certificaciones;
    @Mock
    private DirectorioDeCuentas directorio;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private PdfService pdfService;
    @Mock
    private AsignacionDeInstructores asignacion;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private ProfesorService servicio;
    private final AtomicReference<Profesor> guardado = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        ValidacionDeCertificaciones validacion = new ValidacionDeCertificaciones(certificaciones);
        CertificacionDeInstructores certificacionService = new CertificacionDeInstructores(certificaciones, validacion, clock);
        servicio = new ProfesorService(repositorio, new MapeoDePersona(), directorio, passwordEncoder, eventos,
                pdfService, certificacionService, validacion, asignacion, clock);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash-de-prueba");
        lenient().when(repositorio.saveAndFlush(any(Profesor.class))).thenAnswer(inv -> {
            Profesor profesor = inv.getArgument(0);
            if (profesor.getId() == null) {
                profesor.setId(50L);
            }
            guardado.set(profesor);
            return profesor;
        });
        lenient().when(repositorio.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(guardado.get()));
        lenient().when(certificaciones.save(any(Certificacion.class))).thenAnswer(inv -> {
            Certificacion certificacion = inv.getArgument(0);
            if (certificacion.getId() == null) {
                certificacion.setId(100L);
            }
            return certificacion;
        });
    }

    private CertificacionRequest certificacionVigente() {
        return new CertificacionRequest("CERT-001", "FAT", HOY.minusYears(1), HOY.plusYears(1), null);
    }

    private ProfesorRequest profesorValido() {
        return new ProfesorRequest("Ana", "Gómez", "30123456", null, "Av. Siempreviva", "742", "11",
                "12345678", "ana@correo.com", "Clave-segura-123", "Profesor Nacional de Educación Física",
                certificacionVigente(), "Tenis infantil", null, null);
    }

    private Profesor profesorExistente(Long id, EstadoValidacion estado) {
        Profesor profesor = new Profesor();
        profesor.setId(id);
        profesor.setDni("30123456");
        profesor.setEmail("ana@correo.com");
        profesor.setTituloHabilitante("Profesor Nacional de Educación Física");
        Certificacion certificacion = new Certificacion();
        certificacion.setId(100L);
        certificacion.setNumeroCertificacion("CERT-001");
        certificacion.setFechaVencimiento(HOY.plusYears(1));
        profesor.setCertificacion(certificacion);
        profesor.setEstadoValidacion(estado);
        guardado.set(profesor);
        when(repositorio.findById(id)).thenAnswer(inv -> Optional.ofNullable(guardado.get()));
        return profesor;
    }

    @Test
    void registrar_altaCompleta_naceEnPendiente() {
        ProfesorResponse respuesta = servicio.registrar(profesorValido(), EMPLEADO);

        assertThat(respuesta.rol()).isEqualTo(Rol.PROFESOR);
        assertThat(respuesta.tituloHabilitante()).isEqualTo("Profesor Nacional de Educación Física");
        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.PENDIENTE);
        assertThat(respuesta.certificacion().numeroCertificacion()).isEqualTo("CERT-001");
    }

    @Test
    void registrar_sinTitulo_422() {
        ProfesorRequest sinTitulo = new ProfesorRequest("Ana", "Gómez", "30123456", null, "Av. Siempreviva", "742",
                "11", "12345678", "ana@correo.com", "Clave-segura-123", "", certificacionVigente(), null, null, null);

        assertThatThrownBy(() -> servicio.registrar(sinTitulo, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("PROFESOR_SIN_TITULO");
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void registrar_certificacionVencida_422() {
        CertificacionRequest vencida = new CertificacionRequest("CERT-002", "FAT", HOY.minusYears(2), HOY.minusDays(1), null);
        ProfesorRequest request = new ProfesorRequest("Ana", "Gómez", "30123456", null, "Av. Siempreviva", "742",
                "11", "12345678", "ana@correo.com", "Clave-segura-123", "Título", vencida, null, null, null);

        assertThatThrownBy(() -> servicio.registrar(request, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_VENCIDA");
    }

    @Test
    void registrar_numeroDeCertificacionRepetido_409() {
        when(certificaciones.existsByNumeroCertificacionIgnoreCase("CERT-001")).thenReturn(true);

        // La unicidad del número la valida CertificacionDeInstructores.registrar después de crear la persona;
        // la transacción completa se revierte igual.
        assertThatThrownBy(() -> servicio.registrar(profesorValido(), EMPLEADO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_DUPLICADA");
    }

    @Test
    void modificar_cambiaElTitulo_vuelveAPendienteYLimpiaLaFecha() {
        Profesor profesor = profesorExistente(5L, EstadoValidacion.VALIDADO);
        profesor.setFechaValidacion(HOY.minusDays(10));
        profesor.setValidadoPorId(1L);
        ProfesorUpdateRequest cambios = new ProfesorUpdateRequest(null, null, null, null, null, null, null, null, null,
                "Otro título habilitante", null, null, null, null);

        ProfesorResponse respuesta = servicio.modificar(5L, cambios, EMPLEADO);

        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.PENDIENTE);
        assertThat(profesor.getFechaValidacion()).isNull();
        assertThat(profesor.getValidadoPorId()).isNull();
    }

    @Test
    void modificar_sinTocarElTitulo_noResetea() {
        Profesor profesor = profesorExistente(5L, EstadoValidacion.VALIDADO);
        profesor.setFechaValidacion(HOY.minusDays(10));
        ProfesorUpdateRequest cambios = new ProfesorUpdateRequest(null, null, null, null, null, null, null, null, null,
                null, null, "Tenis avanzado", null, null);

        ProfesorResponse respuesta = servicio.modificar(5L, cambios, EMPLEADO);

        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
        assertThat(profesor.getFechaValidacion()).isEqualTo(HOY.minusDays(10));
    }


    @Test
    void decidirCertificacion_aprobarUnoPendiente_pasaAValidado() {
        profesorExistente(5L, EstadoValidacion.PENDIENTE);
        Actor admin = new Actor(1L, Rol.ADMINISTRADOR);

        ProfesorResponse respuesta = servicio.decidirCertificacion(5L, new ValidacionRequest(true, null), admin);

        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
        assertThat(respuesta.fechaValidacion()).isEqualTo(HOY);
    }

    @Test
    void decidirCertificacion_aprobarDosVeces_esIdempotente() {
        profesorExistente(5L, EstadoValidacion.PENDIENTE);
        Actor admin = new Actor(1L, Rol.ADMINISTRADOR);

        servicio.decidirCertificacion(5L, new ValidacionRequest(true, null), admin);
        servicio.decidirCertificacion(5L, new ValidacionRequest(true, null), admin);

        verify(repositorio, org.mockito.Mockito.times(2)).saveAndFlush(any());
    }

    @Test
    void decidirCertificacion_aprobarUnaVencida_422() {
        Profesor profesor = profesorExistente(5L, EstadoValidacion.PENDIENTE);
        profesor.getCertificacion().setFechaVencimiento(HOY.minusDays(1));
        Actor admin = new Actor(1L, Rol.ADMINISTRADOR);

        assertThatThrownBy(() -> servicio.decidirCertificacion(5L, new ValidacionRequest(true, null), admin))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_VENCIDA");
    }

    @Test
    void decidirCertificacion_rechazarConMotivo_pasaARechazada() {
        profesorExistente(5L, EstadoValidacion.PENDIENTE);
        Actor admin = new Actor(1L, Rol.ADMINISTRADOR);

        ProfesorResponse respuesta = servicio.decidirCertificacion(5L, new ValidacionRequest(false, "El archivo no es legible"), admin);

        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.RECHAZADA);
    }

    @Test
    void eliminar_conHistorial_409() {
        Profesor profesor = profesorExistente(5L, EstadoValidacion.VALIDADO);
        doThrow(new DataIntegrityViolationException("fk")).when(repositorio).delete(profesor);

        assertThatThrownBy(() -> servicio.eliminar(5L, new Actor(1L, Rol.ADMINISTRADOR)))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("PERSONA_CON_HISTORIAL");
    }


    @Test
    void buscar_devuelveProfesorResponseConEstadoYTitulo() {
        Profesor profesor = new Profesor();
        profesor.setId(7L);
        profesor.setTituloHabilitante("Profesor Nacional de Educación Física");
        profesor.setEstadoValidacion(EstadoValidacion.VALIDADO);
        Page<Profesor> pagina = new PageImpl<>(List.of(profesor));
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        PageResponse<ProfesorResponse> respuesta = servicio.buscar(null, null, EstadoValidacion.VALIDADO, true, 0, 20);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().get(0).tituloHabilitante()).isEqualTo("Profesor Nacional de Educación Física");
        assertThat(respuesta.contenido().get(0).estadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
    }

    @Test
    void buscar_sinCriterios_listaTodos() {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResponse<ProfesorResponse> respuesta = servicio.buscar(null, null, null, null, 0, 20);

        assertThat(respuesta.contenido()).isEmpty();
    }

    @Test
    void imprimir_incluyeElEstadoDelTitulo() {
        profesorExistente(5L, EstadoValidacion.VALIDADO);
        ArgumentCaptor<List<CampoFicha>> captor = ArgumentCaptor.forClass(List.class);

        servicio.imprimir(5L);

        verify(pdfService).generarFicha(org.mockito.Mockito.eq("Ficha de profesor"), captor.capture());
        assertThat(captor.getValue()).anySatisfy(campo ->
                assertThat(campo.etiqueta()).isEqualTo("Estado del título"));
    }
}
