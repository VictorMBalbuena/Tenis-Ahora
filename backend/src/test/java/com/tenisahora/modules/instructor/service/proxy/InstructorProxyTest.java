package com.tenisahora.modules.instructor.service.proxy;

import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorProxyTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    @Mock
    private ProfesorRepository profesores;
    @Mock
    private EntrenadorRepository entrenadores;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private InstructorProxy proxy;

    @BeforeEach
    void setUp() {
        proxy = new InstructorProxy(profesores, entrenadores, clock);
    }

    private Entrenador con(EstadoValidacion estado, Certificacion certificacion) {
        Entrenador entrenador = new Entrenador();
        entrenador.setId(5L);
        entrenador.setEstadoValidacion(estado);
        entrenador.setCertificacion(certificacion);
        when(entrenadores.findById(5L)).thenReturn(Optional.of(entrenador));
        return entrenador;
    }

    private Certificacion vigente() {
        Certificacion certificacion = new Certificacion();
        certificacion.setFechaVencimiento(HOY.plusYears(1));
        return certificacion;
    }

    private Certificacion vencida() {
        Certificacion certificacion = new Certificacion();
        certificacion.setFechaVencimiento(HOY.minusDays(1));
        return certificacion;
    }

    @Test
    void habilitacion_validadoYVigente_asignable() {
        con(EstadoValidacion.VALIDADO, vigente());

        HabilitacionResponse habilitacion = proxy.habilitacion(Rol.ENTRENADOR, 5L);

        assertThat(habilitacion.asignable()).isTrue();
        assertThat(habilitacion.motivo()).isNull();
    }

    @Test
    void asegurarAsignable_pendiente_422() {
        con(EstadoValidacion.PENDIENTE, vigente());

        assertThatThrownBy(() -> proxy.asegurarAsignable(Rol.ENTRENADOR, 5L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
    }

    @Test
    void asegurarAsignable_rechazada_422() {
        con(EstadoValidacion.RECHAZADA, vigente());

        assertThatThrownBy(() -> proxy.asegurarAsignable(Rol.ENTRENADOR, 5L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
    }

    @Test
    void asegurarAsignable_sinCertificacion_422() {
        con(EstadoValidacion.VALIDADO, null);

        assertThatThrownBy(() -> proxy.asegurarAsignable(Rol.ENTRENADOR, 5L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
    }

    @Test
    void asegurarAsignable_validadoPeroVencido_422() {
        con(EstadoValidacion.VALIDADO, vencida());

        assertThatThrownBy(() -> proxy.asegurarAsignable(Rol.ENTRENADOR, 5L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
    }

    @Test
    void habilitacion_validadoPeroVencido_elMotivoDiceVencida() {
        con(EstadoValidacion.VALIDADO, vencida());

        HabilitacionResponse habilitacion = proxy.habilitacion(Rol.ENTRENADOR, 5L);

        assertThat(habilitacion.asignable()).isFalse();
        assertThat(habilitacion.motivo()).containsIgnoringCase("vencida");
    }
}
