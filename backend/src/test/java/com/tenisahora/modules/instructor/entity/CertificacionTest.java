package com.tenisahora.modules.instructor.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CertificacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    private Certificacion con(LocalDate vencimiento) {
        Certificacion certificacion = new Certificacion();
        certificacion.setFechaVencimiento(vencimiento);
        return certificacion;
    }

    @Test
    void estaVigente_venceHoy_esVigente() {
        assertThat(con(HOY).estaVigente(HOY)).isTrue();
    }

    @Test
    void estaVigente_vencioAyer_noEsVigente() {
        assertThat(con(HOY.minusDays(1)).estaVigente(HOY)).isFalse();
    }

    @Test
    void estaVigente_sinVencimiento_esVigente() {
        assertThat(con(null).estaVigente(HOY)).isTrue();
    }
}
