package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.mercadopago.EstadoDeOrden;
import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.ResultadoDePago;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcreditacionDePagosImplTest {

    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private PasarelaDePagos pasarela;
    @Mock
    private EmisionDeRecibos emisionDeRecibos;
    @Mock
    private ConfirmacionDeReserva confirmacionDeReserva;
    @Mock
    private CierreDeOrden cierreDeOrden;
    @InjectMocks
    private AcreditacionDePagosImpl acreditacion;

    @Test
    void acreditar_pagoAcreditadoConElMontoCorrecto_pagaYConfirmaLaReserva() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.ACREDITADO, new BigDecimal("8000.00"), "186778732035"));

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
        assertThat(cobro.getNumeroOperacion()).isEqualTo("186778732035");
        verify(emisionDeRecibos).emitir(cobro);
        verify(confirmacionDeReserva).confirmarPagoAcreditado(cobro.getReserva().getId(), new BigDecimal("8000.00"));
    }

    @Test
    void acreditar_pagoAprobado_cierraElLinkParaQueNoSePagueDosVeces() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.ACREDITADO, new BigDecimal("8000.00"), "186778732035"));

        acreditacion.acreditar("orden-1");

        verify(cierreDeOrden).cerrar("orden-1");
    }

    @Test
    void acreditar_reintentoAprobadoTrasUnRechazo_igualSeAcredita() {
        Cobro cobro = cobro(7L, EstadoCobro.RECHAZADO, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.ACREDITADO, new BigDecimal("8000.00"), "186778732035"));

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
    }

    @Test
    void acreditar_conLaReservaYaCancelada_registraElPagoYNoFalla() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.ACREDITADO, new BigDecimal("8000.00"), "186778732035"));
        when(confirmacionDeReserva.confirmarPagoAcreditado(any(), any())).thenReturn(false);

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
        verify(emisionDeRecibos).emitir(cobro);
    }

    @Test
    void acreditar_avisoRepetido_A2_unSoloCobroYUnSoloRecibo() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));

        acreditacion.acreditar("orden-1");

        verify(pasarela, never()).consultar(any());
        verify(emisionDeRecibos, never()).emitir(any());
        verify(confirmacionDeReserva, never()).confirmarPagoAcreditado(any(), any());
    }

    @Test
    void acreditar_pagoRechazado_A3_sigueEsperandoYQuedaRechazado() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.RECHAZADO, BigDecimal.ZERO, "186778732035"));

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.RECHAZADO);
        verify(confirmacionDeReserva, never()).confirmarPagoAcreditado(any(), any());
    }

    @Test
    void acreditar_pagoTodaviaPendiente_noCambiaNada() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.PENDIENTE, BigDecimal.ZERO, null));

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PENDIENTE);
        verify(cobroRepository, never()).save(any());
    }

    @Test
    void acreditar_cobroInexistente_noFalla() {
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-x")).thenReturn(Optional.empty());

        acreditacion.acreditar("orden-x");

        verify(pasarela, never()).consultar(any());
    }

    @Test
    void acreditar_montoAcreditadoDistintoDelCobro_noConfirmaNiAcredita() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", ResultadoDePago.ACREDITADO, new BigDecimal("1.00"), "186778732035"));

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PENDIENTE);
        verify(confirmacionDeReserva, never()).confirmarPagoAcreditado(any(), any());
    }
}
