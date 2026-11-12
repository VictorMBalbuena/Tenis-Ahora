package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.infrastructure.mercadopago.OrdenQr;
import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.infrastructure.mercadopago.SolicitudDeOrden;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.service.ReservasProperties;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static com.tenisahora.modules.cobro.DatosDePrueba.AHORA;
import static com.tenisahora.modules.cobro.DatosDePrueba.MP_PROPIEDADES;
import static com.tenisahora.modules.cobro.DatosDePrueba.RELOJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoMercadoPagoTest {

    @Mock
    private PasarelaDePagos pasarela;

    private static final ReservasProperties RESERVAS = new ReservasProperties(30, 2, 6, 50, 100, 30, 30, 30, 10, 20);

    private PagoMercadoPago pago;

    @BeforeEach
    void setUp() {
        pago = new PagoMercadoPago(pasarela, MP_PROPIEDADES, RESERVAS, RELOJ);
    }

    private Cobro cobro() {
        return cobroCreadoHace(0);
    }

    private Cobro cobroCreadoHace(int minutos) {
        Reserva reserva = new Reserva();
        reserva.setFechaCreacion(AHORA.minusMinutes(minutos));
        Cobro cobro = new Cobro();
        cobro.setReserva(reserva);
        cobro.setId(5L);
        cobro.setCodigo("C-000005");
        cobro.setMontoFinal(new BigDecimal("8000.00"));
        return cobro;
    }

    @Test
    void medio_qr() {
        assertThat(pago.medio()).isEqualTo(MedioPago.QR);
    }

    @Test
    void cobrar_pideLaOrdenAlPuertoYDejaPendienteConElQr() {
        Instant venceEn = RELOJ.instant().plusSeconds(600);
        when(pasarela.crearOrdenQr(any())).thenReturn(new OrdenQr("orden-1", "data-del-qr", venceEn));

        ResultadoDeCobro resultado = pago.cobrar(cobro(), new DatosDeCobro(null, null, true, null));

        assertThat(resultado.estado()).isEqualTo(EstadoCobro.PENDIENTE);
        assertThat(resultado.idPagoExterno()).isEqualTo("orden-1");
        assertThat(resultado.qrData()).isEqualTo("data-del-qr");
        assertThat(resultado.fechaExpiracionQr()).isNotNull();
    }

    @Test
    void cobrar_conPocoPlazoDeLaReserva_elQrNoDuraMasQueElla() {
        when(pasarela.crearOrdenQr(any())).thenReturn(new OrdenQr("orden-1", "data-del-qr", RELOJ.instant()));

        pago.cobrar(cobroCreadoHace(25), new DatosDeCobro(null, null, true, null));

        ArgumentCaptor<SolicitudDeOrden> solicitud = ArgumentCaptor.forClass(SolicitudDeOrden.class);
        verify(pasarela).crearOrdenQr(solicitud.capture());
        assertThat(solicitud.getValue().vigencia()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void cobrar_unQrQueReemplazaAOtro_usaOtraClaveDeIdempotencia() {
        when(pasarela.crearOrdenQr(any())).thenReturn(new OrdenQr("orden-1", "data-del-qr", RELOJ.instant()));
        Cobro conQrAnterior = cobro();
        conQrAnterior.setIdPagoExterno("orden-anterior");

        pago.cobrar(cobro(), new DatosDeCobro(null, null, true, null));
        pago.cobrar(conQrAnterior, new DatosDeCobro(null, null, true, null));

        ArgumentCaptor<SolicitudDeOrden> solicitud = ArgumentCaptor.forClass(SolicitudDeOrden.class);
        verify(pasarela, times(2)).crearOrdenQr(solicitud.capture());
        assertThat(solicitud.getAllValues().get(0).claveDeIdempotencia()).isEqualTo("cobro-5");
        assertThat(solicitud.getAllValues().get(1).claveDeIdempotencia()).startsWith("cobro-5-");
    }

    @Test
    void cobrar_conElPlazoDeLaReservaTerminado_422() {
        assertThatThrownBy(() -> pago.cobrar(cobroCreadoHace(30), new DatosDeCobro(null, null, true, null)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void cobrar_puertoCaido_PasarelaNoDisponible() {
        when(pasarela.crearOrdenQr(any())).thenThrow(new PasarelaNoDisponibleException("no responde"));

        assertThatThrownBy(() -> pago.cobrar(cobro(), new DatosDeCobro(null, null, true, null)))
                .isInstanceOf(PasarelaNoDisponibleException.class);
    }
}
