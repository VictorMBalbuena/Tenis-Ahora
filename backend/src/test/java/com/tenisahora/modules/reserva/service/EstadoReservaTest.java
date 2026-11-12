package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoReservaTest {

    // ---------- Estados ----------

    @Test
    void pendienteConfirmadaYEnCursoRetienenElHorario_canceladaYFinalizadaNo() {
        assertThat(EstadoReserva.PENDIENTE_PAGO.retieneHorario()).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.retieneHorario()).isTrue();
        assertThat(EstadoReserva.EN_CURSO.retieneHorario()).isTrue();
        assertThat(EstadoReserva.CANCELADA.retieneHorario()).isFalse();
        assertThat(EstadoReserva.FINALIZADA.retieneHorario()).isFalse();
        assertThat(EstadoReserva.queRetienenHorario()).containsExactly(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);
    }

    @Test
    void soloLasPendientesYConfirmadasSePuedenModificarOCancelar_unaEnCursoYaNo() {
        assertThat(EstadoReserva.PENDIENTE_PAGO.puedeModificarse()).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.puedeModificarse()).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.puedeCancelarse()).isTrue();
        assertThat(EstadoReserva.EN_CURSO.puedeModificarse()).isFalse();
        assertThat(EstadoReserva.EN_CURSO.puedeCancelarse()).isFalse();
        assertThat(EstadoReserva.CANCELADA.puedeCancelarse()).isFalse();
        assertThat(EstadoReserva.FINALIZADA.puedeModificarse()).isFalse();
    }

    @Test
    void soloUnaConfirmadaSeInicia_yDeUnaEnCursoSeFinaliza() {
        for (EstadoReserva estado : EstadoReserva.values()) {
            assertThat(estado.puedeIniciarse()).isEqualTo(estado == EstadoReserva.CONFIRMADA);
            assertThat(estado.puedeFinalizarse()).isEqualTo(estado == EstadoReserva.EN_CURSO);
        }
    }
}
