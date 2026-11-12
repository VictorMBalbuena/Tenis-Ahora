package com.tenisahora.infrastructure.mercadopago;

import java.math.BigDecimal;

/**
 * Lo que Mercado Pago responde cuando se le pregunta por una orden.
 *
 * @param idExterno    identificador de la orden en Mercado Pago
 * @param resultado    cómo quedó el pago
 * @param montoPagado  lo que efectivamente se pagó; se compara contra el monto del cobro antes de acreditar,
 *                     así un pago por menos plata no confirma la reserva
 * @param idPago       número de operación del pago en Mercado Pago, el mismo del comprobante; null si todavía no hay pago
 */
public record EstadoDeOrden(String idExterno, ResultadoDePago resultado, BigDecimal montoPagado, String idPago) {
}
