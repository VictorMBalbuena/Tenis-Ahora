package com.tenisahora.infrastructure.mercadopago;

import java.time.Instant;

/**
 * El QR que devolvió Mercado Pago.
 *
 * @param idExterno identificador de la orden en Mercado Pago; es con lo que después se la consulta
 * @param qrData    el contenido del QR, que el frontend dibuja para que lo escaneen
 * @param venceEn   momento exacto en que deja de servir; se guarda como Instant para que no lo corra la zona horaria
 */
public record OrdenQr(String idExterno, String qrData, Instant venceEn) {
}
