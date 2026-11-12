package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CertificacionDeInstructoresTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final Actor ADMIN = new Actor(1L, Rol.ADMINISTRADOR);

    @Mock
    private CertificacionRepository certificaciones;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private CertificacionDeInstructores servicio;

    @BeforeEach
    void setUp() {
        servicio = new CertificacionDeInstructores(certificaciones, new ValidacionDeCertificaciones(certificaciones), clock);
        lenient().when(certificaciones.save(any(Certificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Entrenador pendiente(LocalDate vencimiento) {
        Entrenador entrenador = new Entrenador();
        entrenador.setId(5L);
        Certificacion certificacion = new Certificacion();
        certificacion.setId(100L);
        certificacion.setNumeroCertificacion("CERT-001");
        certificacion.setFechaVencimiento(vencimiento);
        entrenador.setCertificacion(certificacion);
        entrenador.setEstadoValidacion(EstadoValidacion.PENDIENTE);
        return entrenador;
    }

    @Test
    void decidir_aprobarUnaVigente_pasaAValidadoConFechaYValidador() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));

        servicio.decidir(entrenador, new ValidacionRequest(true, null), ADMIN);

        assertThat(entrenador.getEstadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
        assertThat(entrenador.getFechaValidacion()).isEqualTo(HOY);
        assertThat(entrenador.getValidadoPorId()).isEqualTo(1L);
    }

    @Test
    void decidir_aprobarUnaVencida_422() {
        Entrenador entrenador = pendiente(HOY.minusDays(1));

        assertThatThrownBy(() -> servicio.decidir(entrenador, new ValidacionRequest(true, null), ADMIN))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_VENCIDA");
    }

    @Test
    void decidir_aprobarDosVeces_esIdempotente() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));

        servicio.decidir(entrenador, new ValidacionRequest(true, null), ADMIN);
        servicio.decidir(entrenador, new ValidacionRequest(true, null), ADMIN);

        assertThat(entrenador.getEstadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
    }

    @Test
    void decidir_rechazarUnaYaValidada_422() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));
        servicio.decidir(entrenador, new ValidacionRequest(true, null), ADMIN);

        assertThatThrownBy(() -> servicio.decidir(entrenador, new ValidacionRequest(false, "Motivo"), ADMIN))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_YA_DECIDIDA");
    }

    @Test
    void decidir_rechazarConMotivo_pasaARechazadaYGuardaElMotivo() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));

        servicio.decidir(entrenador, new ValidacionRequest(false, "El archivo no es legible"), ADMIN);

        assertThat(entrenador.getEstadoValidacion()).isEqualTo(EstadoValidacion.RECHAZADA);
        assertThat(entrenador.getMotivoRechazo()).isEqualTo("El archivo no es legible");
        assertThat(entrenador.getValidadoPorId()).isEqualTo(1L);
    }

    @Test
    void decidir_rechazarSinMotivo_422() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));

        assertThatThrownBy(() -> servicio.decidir(entrenador, new ValidacionRequest(false, "  "), ADMIN))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("MOTIVO_DE_RECHAZO_REQUERIDO");
    }

    @Test
    void decidir_unaRechazada_sePuedeVolverADecidirDespuesDeReemplazarElTitulo() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));
        servicio.decidir(entrenador, new ValidacionRequest(false, "Motivo"), ADMIN);

        servicio.reemplazar(entrenador, new CertificacionRequest("CERT-002", "FAT", HOY.minusMonths(1), HOY.plusYears(2), null));
        servicio.decidir(entrenador, new ValidacionRequest(true, null), ADMIN);

        assertThat(entrenador.getEstadoValidacion()).isEqualTo(EstadoValidacion.VALIDADO);
        assertThat(entrenador.getMotivoRechazo()).isNull();
    }

    @Test
    void registrar_certificacionNueva_quedaPendiente() {
        Entrenador entrenador = new Entrenador();
        entrenador.setId(5L);

        servicio.registrar(entrenador, new CertificacionRequest("CERT-001", "FAT", HOY.minusYears(1), HOY.plusYears(1), null));

        assertThat(entrenador.getEstadoValidacion()).isEqualTo(EstadoValidacion.PENDIENTE);
        assertThat(entrenador.getCertificacion().getNumeroCertificacion()).isEqualTo("CERT-001");
    }

    @Test
    void reemplazar_vuelveAPendienteYLimpiaFechaValidadorYMotivo() {
        Entrenador entrenador = pendiente(HOY.plusYears(1));
        servicio.decidir(entrenador, new ValidacionRequest(false, "Motivo"), ADMIN);

        servicio.reemplazar(entrenador, new CertificacionRequest("CERT-002", "FAT", HOY.minusMonths(1), HOY.plusYears(2), null));

        assertThat(entrenador.getEstadoValidacion()).isEqualTo(EstadoValidacion.PENDIENTE);
        assertThat(entrenador.getFechaValidacion()).isNull();
        assertThat(entrenador.getValidadoPorId()).isNull();
        assertThat(entrenador.getMotivoRechazo()).isNull();
    }
}
