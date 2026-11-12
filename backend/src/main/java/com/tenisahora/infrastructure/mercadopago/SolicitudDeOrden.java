package com.tenisahora.infrastructure.mercadopago;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Lo que hay que mandarle a Mercado Pago para que devuelva un QR.
 *
 * @param monto             importe a cobrar, calculado siempre por el servidor (RD09)
 * @param referenciaExterna el código del cobro, para reconocerlo cuando llegue el aviso
 * @param vigencia          cuánto dura el QR antes de vencer (10 minutos)
 * @param claveDeIdempotencia identificador del pedido: si se reintenta, Mercado Pago no crea una orden nueva
 */
public record SolicitudDeOrden(BigDecimal monto, String referenciaExterna, Duration vigencia,
                               String claveDeIdempotencia) {
}
