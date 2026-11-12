package com.tenisahora.infrastructure.mercadopago;

import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class MercadoPagoClientTest {

    private static final OffsetDateTime ORDEN_CREADA = OffsetDateTime.parse("2026-11-07T10:00:00-03:00");

    private Payment pago(String estado, OffsetDateTime fecha) {
        Payment pago = mock(Payment.class);
        lenient().when(pago.getStatus()).thenReturn(estado);
        lenient().when(pago.getDateCreated()).thenReturn(fecha);
        return pago;
    }

    @Test
    void mejorPago_unRechazoAnteriorALaOrden_noCuenta() {
        Payment rechazadoAntes = pago("rejected", ORDEN_CREADA.minusMinutes(5));

        assertThat(MercadoPagoClient.mejorPago(List.of(rechazadoAntes), ORDEN_CREADA)).isEmpty();
    }

    @Test
    void mejorPago_unRechazoPosteriorALaOrden_cuenta() {
        Payment rechazadoDespues = pago("rejected", ORDEN_CREADA.plusMinutes(1));

        assertThat(MercadoPagoClient.mejorPago(List.of(rechazadoDespues), ORDEN_CREADA)).contains(rechazadoDespues);
    }

    @Test
    void mejorPago_sinPagos_vacio() {
        assertThat(MercadoPagoClient.mejorPago(List.of(), ORDEN_CREADA)).isEmpty();
    }

    @Test
    void mejorPago_creadoEnElMismoInstanteQueLaOrden_cuenta() {
        Payment alMismoTiempo = pago("rejected", ORDEN_CREADA);

        assertThat(MercadoPagoClient.mejorPago(List.of(alMismoTiempo), ORDEN_CREADA)).contains(alMismoTiempo);
    }

    @Test
    void mejorPago_sinFechaDeLaOrden_cuentanTodos() {
        Payment rechazado = pago("rejected", ORDEN_CREADA.minusDays(1));

        assertThat(MercadoPagoClient.mejorPago(List.of(rechazado), null)).contains(rechazado);
    }

    @Test
    void mejorPago_variosDelMismoEstado_elMasReciente() {
        Payment primero = pago("rejected", ORDEN_CREADA.plusMinutes(1));
        Payment segundo = pago("rejected", ORDEN_CREADA.plusMinutes(2));

        assertThat(MercadoPagoClient.mejorPago(List.of(primero, segundo), ORDEN_CREADA)).contains(segundo);
    }

    @Test
    void mejorPago_unAprobadoAnteriorALaOrden_igualGana() {
        Payment aprobadoAntes = pago("approved", ORDEN_CREADA.minusMinutes(5));
        Payment rechazadoDespues = pago("rejected", ORDEN_CREADA.plusMinutes(1));

        assertThat(MercadoPagoClient.mejorPago(List.of(aprobadoAntes, rechazadoDespues), ORDEN_CREADA)).contains(aprobadoAntes);
    }
}
