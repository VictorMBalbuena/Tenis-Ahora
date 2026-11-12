package com.tenisahora.infrastructure.mercadopago;

/** Cómo quedó un pago según Mercado Pago, traducido a los cuatro casos que al sistema le cambian algo. */
public enum ResultadoDePago {
    ACREDITADO,
    PENDIENTE,
    RECHAZADO,
    VENCIDO
}
