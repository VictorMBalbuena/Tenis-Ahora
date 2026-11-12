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

import static com.tenisahora.modules.cobro.DatosDePrueba.AHORA;
import static com.tenisahora.modules.cobro.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciliacionDePagosTest {

    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private AcreditacionDePagos acreditacionDePagos;

    private ConciliacionDePagos conciliacion;

    @BeforeEach
    void setUp() {
        conciliacion = new ConciliacionDePagos(cobroRepository, acreditacionDePagos, RELOJ);
    }

    @Test
    void conciliar_recuperaUnPagoCuyoAvisoSePerdio() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroRepository.pendientesDeConciliar(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of(cobro));

        int revisados = conciliacion.conciliar();

        assertThat(revisados).isEqualTo(1);
        verify(acreditacionDePagos).acreditar("orden-1");
    }

    @Test
    void conciliar_sinPendientesConQrVencido_noTocaNada() {
        when(cobroRepository.pendientesDeConciliar(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of());

        assertThat(conciliacion.conciliar()).isZero();
    }

    @Test
    void conciliar_unFalloEnUnPago_noCortaElLote() {
        Cobro falla = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        falla.setIdPagoExterno("orden-1");
        Cobro ok = cobro(8L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        ok.setIdPagoExterno("orden-2");
        when(cobroRepository.pendientesDeConciliar(List.of(EstadoCobro.PENDIENTE), AHORA)).thenReturn(List.of(falla, ok));
        org.mockito.Mockito.doThrow(new RuntimeException("no responde")).when(acreditacionDePagos).acreditar("orden-1");

        int revisados = conciliacion.conciliar();

        assertThat(revisados).isEqualTo(1);
        verify(acreditacionDePagos).acreditar("orden-2");
    }
}
