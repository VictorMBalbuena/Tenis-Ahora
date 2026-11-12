package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmacionDeReservaImplTest {

    @Mock
    private ReservaRepository reservaRepository;
    @InjectMocks
    private ConfirmacionDeReservaImpl confirmacion;

    private Reserva con(EstadoReserva estado) {
        Reserva reserva = reserva(10L, estado, HOY.plusDays(3), LocalTime.of(18, 0));
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        return reserva;
    }

    // ---------- Confirmación (la usa BACK 08) ----------

    @Test
    void confirmar_unaPendiente_laPasaAConfirmada() {
        Reserva reserva = con(EstadoReserva.PENDIENTE_PAGO);

        confirmacion.confirmar(10L);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void confirmar_unaYaConfirmada_noHaceNada() {
        con(EstadoReserva.CONFIRMADA);

        confirmacion.confirmar(10L);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void confirmar_unaCanceladaOFinalizada_422() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.CANCELADA, EstadoReserva.FINALIZADA}) {
            con(estado);

            assertThatThrownBy(() -> confirmacion.confirmar(10L))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_NO_MODIFICABLE");
        }
    }

    @Test
    void confirmarPagoAcreditado_unaCancelada_dejaElReembolsoYDevuelveFalse() {
        Reserva reserva = con(EstadoReserva.CANCELADA);

        assertThat(confirmacion.confirmarPagoAcreditado(10L, new java.math.BigDecimal("8000.00"))).isFalse();
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reserva.getMontoReembolso()).isEqualByComparingTo("8000.00");
    }

    @Test
    void confirmarPagoAcreditado_unaPendiente_laConfirma() {
        Reserva reserva = con(EstadoReserva.PENDIENTE_PAGO);

        assertThat(confirmacion.confirmarPagoAcreditado(10L, new java.math.BigDecimal("8000.00"))).isTrue();
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void confirmar_unaInexistente_404() {
        when(reservaRepository.buscarParaModificar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmacion.confirmar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
