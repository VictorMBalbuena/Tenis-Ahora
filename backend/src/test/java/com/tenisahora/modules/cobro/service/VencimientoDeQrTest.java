package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.AHORA;
import static com.tenisahora.modules.cobro.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VencimientoDeQrTest {

    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private AcreditacionDePagos acreditacionDePagos;

    private VencimientoDeQr vencimiento;

    @BeforeEach
    void setUp() {
        vencimiento = new VencimientoDeQr(cobroRepository, new VencimientoDeUnQr(cobroRepository, acreditacionDePagos), RELOJ);
    }

    @Test
    void vencerExpirados_alos9con59_sigueEsperandoYNoLoToca() {
        when(cobroRepository.conQrVencido(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of());

        assertThat(vencimiento.vencerExpirados()).isZero();
    }

    @Test
    void vencerExpirados_alos10con01_pasaAVencido() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.conQrVencido(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of(cobro));
        when(cobroRepository.buscarParaModificar(7L)).thenReturn(Optional.of(cobro));

        int vencidos = vencimiento.vencerExpirados();

        assertThat(vencidos).isEqualTo(1);
        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.VENCIDO);
    }

    @Test
    void vencerExpirados_unCobroYaPagado_noSeVence() {
        Cobro pagado = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        when(cobroRepository.conQrVencido(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of(pagado));
        when(cobroRepository.buscarParaModificar(7L)).thenReturn(Optional.of(pagado));

        vencimiento.vencerExpirados();

        assertThat(pagado.getEstado()).isEqualTo(EstadoCobro.PAGADO);
    }

    @Test
    void vencerExpirados_sePagoJustoAntesDeVencer_noLoMarcaVencido() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroRepository.conQrVencido(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of(cobro));
        when(cobroRepository.buscarParaModificar(7L)).thenReturn(Optional.of(cobro));
        // Simula que, al volver a consultar a Mercado Pago, el pago ya se había acreditado.
        org.mockito.Mockito.doAnswer(inv -> {
            cobro.setEstado(EstadoCobro.PAGADO);
            return null;
        }).when(acreditacionDePagos).acreditar("orden-1");

        vencimiento.vencerExpirados();

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
    }

    @Test
    void vencerExpirados_unFalloEnUnItem_noCortaElLote() {
        Cobro falla = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        Cobro ok = cobro(8L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.conQrVencido(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of(falla, ok));
        when(cobroRepository.buscarParaModificar(7L)).thenThrow(new RuntimeException("falló"));
        when(cobroRepository.buscarParaModificar(8L)).thenReturn(Optional.of(ok));

        int vencidos = vencimiento.vencerExpirados();

        assertThat(vencidos).isEqualTo(1);
        assertThat(ok.getEstado()).isEqualTo(EstadoCobro.VENCIDO);
    }
}
