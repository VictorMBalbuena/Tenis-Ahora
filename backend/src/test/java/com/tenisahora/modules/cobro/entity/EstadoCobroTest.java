package com.tenisahora.modules.cobro.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoCobroTest {

    @Test
    void puedeGenerarQr_pendienteVencidoORechazado() {
        assertThat(EstadoCobro.PENDIENTE.puedeGenerarQr()).isTrue();
        assertThat(EstadoCobro.VENCIDO.puedeGenerarQr()).isTrue();
        assertThat(EstadoCobro.RECHAZADO.puedeGenerarQr()).isTrue();
        assertThat(EstadoCobro.PAGADO.puedeGenerarQr()).isFalse();
        assertThat(EstadoCobro.ANULADO.puedeGenerarQr()).isFalse();
    }

    @Test
    void puedeAcreditarse_soloPendiente() {
        assertThat(EstadoCobro.PENDIENTE.puedeAcreditarse()).isTrue();
        for (EstadoCobro estado : new EstadoCobro[]{EstadoCobro.PAGADO, EstadoCobro.VENCIDO, EstadoCobro.RECHAZADO, EstadoCobro.ANULADO}) {
            assertThat(estado.puedeAcreditarse()).isFalse();
        }
    }

    @Test
    void esFinal_pagadoOAnulado() {
        assertThat(EstadoCobro.PAGADO.esFinal()).isTrue();
        assertThat(EstadoCobro.ANULADO.esFinal()).isTrue();
        assertThat(EstadoCobro.PENDIENTE.esFinal()).isFalse();
        assertThat(EstadoCobro.VENCIDO.esFinal()).isFalse();
        assertThat(EstadoCobro.RECHAZADO.esFinal()).isFalse();
    }

    @Test
    void emiteRecibo_soloPagado() {
        assertThat(EstadoCobro.PAGADO.emiteRecibo()).isTrue();
        for (EstadoCobro estado : new EstadoCobro[]{EstadoCobro.PENDIENTE, EstadoCobro.VENCIDO, EstadoCobro.RECHAZADO, EstadoCobro.ANULADO}) {
            assertThat(estado.emiteRecibo()).isFalse();
        }
    }

    @Test
    void bloqueaNuevoCobro_pendienteOPagado() {
        assertThat(EstadoCobro.PENDIENTE.bloqueaNuevoCobro()).isTrue();
        assertThat(EstadoCobro.PAGADO.bloqueaNuevoCobro()).isTrue();
        assertThat(EstadoCobro.VENCIDO.bloqueaNuevoCobro()).isFalse();
        assertThat(EstadoCobro.RECHAZADO.bloqueaNuevoCobro()).isFalse();
        assertThat(EstadoCobro.ANULADO.bloqueaNuevoCobro()).isFalse();
    }

    @Test
    void queEsperanAcreditacion_soloPendiente() {
        assertThat(EstadoCobro.queEsperanAcreditacion()).containsExactly(EstadoCobro.PENDIENTE);
    }

    @Test
    void queBloqueanNuevoCobro_pendienteYPagado() {
        assertThat(EstadoCobro.queBloqueanNuevoCobro()).containsExactlyInAnyOrder(EstadoCobro.PENDIENTE, EstadoCobro.PAGADO);
    }
}
