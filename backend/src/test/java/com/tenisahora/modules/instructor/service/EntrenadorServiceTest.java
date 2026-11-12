package com.tenisahora.modules.instructor.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.EntrenadorRequest;
import com.tenisahora.modules.instructor.dto.EntrenadorResponse;
import com.tenisahora.modules.instructor.dto.EntrenadorUpdateRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
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
class EntrenadorServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private EntrenadorRepository repositorio;
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
    private EntrenadorService servicio;
    private final AtomicReference<Entrenador> guardado = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        ValidacionDeCertificaciones validacion = new ValidacionDeCertificaciones(certificaciones);
        CertificacionDeInstructores certificacionService = new CertificacionDeInstructores(certificaciones, validacion, clock);
        servicio = new EntrenadorService(repositorio, new MapeoDePersona(), directorio, passwordEncoder, eventos,
                pdfService, certificacionService, validacion, asignacion, clock);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash-de-prueba");
        lenient().when(repositorio.saveAndFlush(any(Entrenador.class))).thenAnswer(inv -> {
            Entrenador entrenador = inv.getArgument(0);
            if (entrenador.getId() == null) {
                entrenador.setId(50L);
            }
            guardado.set(entrenador);
            return entrenador;
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

    private EntrenadorRequest entrenadorValido() {
        return new EntrenadorRequest("Lucas", "Pérez", "31123456", null, "Av. Siempreviva", "742", "11",
                "12345678", "lucas@correo.com", "Clave-segura-123", "Entrenador Nacional de Tenis",
                certificacionVigente(), "Tenis adultos", null);
    }

    private Entrenador entrenadorExistente(Long id, EstadoValidacion estado) {
        Entrenador entrenador = new Entrenador();
        entrenador.setId(id);
        entrenador.setDni("31123456");
        entrenador.setEmail("lucas@correo.com");
        entrenador.setCertificacionDeportiva("Entrenador Nacional de Tenis");
        Certificacion certificacion = new Certificacion();
        certificacion.setId(100L);
        certificacion.setNumeroCertificacion("CERT-001");
        certificacion.setFechaVencimiento(HOY.plusYears(1));
        entrenador.setCertificacion(certificacion);
        entrenador.setEstadoValidacion(estado);
        guardado.set(entrenador);
        when(repositorio.findById(id)).thenAnswer(inv -> Optional.ofNullable(guardado.get()));
        return entrenador;
    }

    @Test
    void registrar_altaCompleta_naceEnPendiente() {
        EntrenadorResponse respuesta = servicio.registrar(entrenadorValido(), EMPLEADO);

        assertThat(respuesta.rol()).isEqualTo(Rol.ENTRENADOR);
        assertThat(respuesta.certificacionDeportiva()).isEqualTo("Entrenador Nacional de Tenis");
        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.PENDIENTE);
        assertThat(respuesta.certificacion().numeroCertificacion()).isEqualTo("CERT-001");
    }

    @Test
    void registrar_sinCertificacionDeportiva_422_A2() {
        EntrenadorRequest sinCertificacion = new EntrenadorRequest("Lucas", "Pérez", "31123456", null,
                "Av. Siempreviva", "742", "11", "12345678", "lucas@correo.com", "Clave-segura-123", "",
                certificacionVigente(), null, null);

        assertThatThrownBy(() -> servicio.registrar(sinCertificacion, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("ENTRENADOR_SIN_CERTIFICACION");
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void registrar_certificacionVencida_422() {
        CertificacionRequest vencida = new CertificacionRequest("CERT-002", "FAT", HOY.minusYears(2), HOY.minusDays(1), null);
        EntrenadorRequest request = new EntrenadorRequest("Lucas", "Pérez", "31123456", null, "Av. Siempreviva", "742",
                "11", "12345678", "lucas@correo.com", "Clave-segura-123", "Certificación", vencida, null, null);

        assertThatThrownBy(() -> servicio.registrar(request, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_VENCIDA");
    }

    @Test
    void registrar_numeroDeCertificacionRepetido_409() {
        when(certificaciones.existsByNumeroCertificacionIgnoreCase("CERT-001")).thenReturn(true);

        // La unicidad del número la valida CertificacionDeInstructores.registrar después de crear la persona:
        // la transacción completa se revierte igual, pero a nivel de mock la persona sí llegó a guardarse una vez.
        assertThatThrownBy(() -> servicio.registrar(entrenadorValido(), EMPLEADO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_DUPLICADA");
    }

    @Test
    void modificar_certificacionNueva_vuelveAPendienteYLimpiaLaFecha_A1() {
        Entrenador entrenador = entrenadorExistente(5L, EstadoValidacion.VALIDADO);
        entrenador.setFechaValidacion(HOY.minusDays(10));
        entrenador.setValidadoPorId(1L);
        entrenador.setMotivoRechazo(null);
        CertificacionRequest nueva = new CertificacionRequest("CERT-003", "FAT", HOY.minusMonths(1), HOY.plusYears(2), null);
        EntrenadorUpdateRequest cambios = new EntrenadorUpdateRequest(null, null, null, null, null, null, null, null,
                null, null, nueva, null, null);

        EntrenadorResponse respuesta = servicio.modificar(5L, cambios, EMPLEADO);

        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.PENDIENTE);
        assertThat(entrenador.getFechaValidacion()).isNull();
        assertThat(entrenador.getValidadoPorId()).isNull();
    }

    @Test
    void modificar_sinTocarLaCertificacion_noResetea() {
        Entrenador entrenador = entrenadorExistente(5L, EstadoValidacion.VALIDADO);
        entrenador.setFechaValidacion(HOY.minusDays(10));
        EntrenadorUpdateRequest cambios = new EntrenadorUpdateRequest(null, null, null, null, null, null, null, null,
                null, null, null, "Tenis avanzado", null);

        EntrenadorResponse respuesta = servicio.modificar(5L, cambios, EMPLEADO);

        assertThat(respuesta.estadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
        assertThat(entrenador.getFechaValidacion()).isEqualTo(HOY.minusDays(10));
    }

    @Test
    void eliminar_conHistorial_409_A1() {
        Entrenador entrenador = entrenadorExistente(5L, EstadoValidacion.VALIDADO);
        doThrow(new DataIntegrityViolationException("fk")).when(repositorio).delete(entrenador);

        assertThatThrownBy(() -> servicio.eliminar(5L, new Actor(1L, Rol.ADMINISTRADOR)))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("PERSONA_CON_HISTORIAL");
    }

    @Test
    void imprimir_incluyeElEstadoDeLaCertificacion() {
        entrenadorExistente(5L, EstadoValidacion.VALIDADO);
        ArgumentCaptor<List<CampoFicha>> captor = ArgumentCaptor.forClass(List.class);

        servicio.imprimir(5L);

        verify(pdfService).generarFicha(org.mockito.Mockito.eq("Ficha de entrenador"), captor.capture());
        assertThat(captor.getValue()).anySatisfy(campo ->
                assertThat(campo.etiqueta()).isEqualTo("Estado de la certificación"));
    }
}
