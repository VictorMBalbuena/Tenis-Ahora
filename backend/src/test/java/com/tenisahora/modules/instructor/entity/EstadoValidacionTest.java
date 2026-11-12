package com.tenisahora.modules.instructor.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoValidacionTest {

    @Test
    void puedeValidarse_pendiente_true() {
        assertThat(EstadoValidacion.PENDIENTE.puedeValidarse()).isTrue();
    }

    @Test
    void puedeValidarse_rechazada_true() {
        assertThat(EstadoValidacion.RECHAZADA.puedeValidarse()).isTrue();
    }

    @Test
    void puedeValidarse_validado_false() {
        assertThat(EstadoValidacion.VALIDADO.puedeValidarse()).isFalse();
    }

    @Test
    void puedeAsignarse_validado_true() {
        assertThat(EstadoValidacion.VALIDADO.puedeAsignarse()).isTrue();
    }

    @Test
    void puedeAsignarse_pendiente_false() {
        assertThat(EstadoValidacion.PENDIENTE.puedeAsignarse()).isFalse();
    }

    @Test
    void puedeAsignarse_rechazada_false() {
        assertThat(EstadoValidacion.RECHAZADA.puedeAsignarse()).isFalse();
    }

    @Test
    void fueRechazada_rechazada_true() {
        assertThat(EstadoValidacion.RECHAZADA.fueRechazada()).isTrue();
    }

    @Test
    void fueRechazada_pendiente_false() {
        assertThat(EstadoValidacion.PENDIENTE.fueRechazada()).isFalse();
    }

    @Test
    void fueRechazada_validado_false() {
        assertThat(EstadoValidacion.VALIDADO.fueRechazada()).isFalse();
    }
}
