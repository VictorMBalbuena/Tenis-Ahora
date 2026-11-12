package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionDeRecibosTest {

    @Mock
    private ReciboRepository reciboRepository;
    @InjectMocks
    private EmisionDeRecibos emision;

    @Test
    void emitir_unCobroPagado_emiteElReciboConLaNumeracion() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, com.tenisahora.modules.cobro.entity.MedioPago.DEBITO, "8000.00");
        when(reciboRepository.findByCobroId(7L)).thenReturn(Optional.empty());
        when(reciboRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Recibo recibo = emision.emitir(cobro);

        assertThat(recibo.getNumeroRecibo()).isEqualTo("R-000007");
        assertThat(recibo.getMonto()).isEqualByComparingTo("8000.00");
        assertThat(recibo.getMedioPago()).isEqualTo(com.tenisahora.modules.cobro.entity.MedioPago.DEBITO);
    }

    @Test
    void emitir_yaTieneRecibo_noEmiteOtro() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, com.tenisahora.modules.cobro.entity.MedioPago.DEBITO, "8000.00");
        Recibo existente = new Recibo();
        when(reciboRepository.findByCobroId(7L)).thenReturn(Optional.of(existente));

        Recibo recibo = emision.emitir(cobro);

        assertThat(recibo).isSameAs(existente);
        verify(reciboRepository, never()).save(any());
    }

    @Test
    void emitir_cobroAnulado_A1_bloquea() {
        Cobro cobro = cobro(7L, EstadoCobro.ANULADO, com.tenisahora.modules.cobro.entity.MedioPago.DEBITO, "8000.00");

        assertThatThrownBy(() -> emision.emitir(cobro))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RECIBO_NO_EMITIBLE");
    }

    @Test
    void emitir_cobroPendiente_bloquea() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, com.tenisahora.modules.cobro.entity.MedioPago.QR, "8000.00");

        assertThatThrownBy(() -> emision.emitir(cobro)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
