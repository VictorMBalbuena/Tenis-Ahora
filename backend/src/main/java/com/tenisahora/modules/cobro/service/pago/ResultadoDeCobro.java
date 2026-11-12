package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.EstadoCobro;

import java.time.LocalDateTime;

/**
 * En qué quedó el intento de cobro. Débito y crédito resuelven {@code numeroOperacion}; Mercado Pago resuelve
 * {@code idPagoExterno}, {@code qrData} y {@code fechaExpiracionQr}. Nunca se completan los tres a la vez.
 */
public record ResultadoDeCobro(EstadoCobro estado, String numeroOperacion, String idPagoExterno, String qrData,
                                LocalDateTime fechaExpiracionQr) {

    public static ResultadoDeCobro pagado(String numeroOperacion) {
        return new ResultadoDeCobro(EstadoCobro.PAGADO, numeroOperacion, null, null, null);
    }

    public static ResultadoDeCobro rechazado(String numeroOperacion) {
        return new ResultadoDeCobro(EstadoCobro.RECHAZADO, numeroOperacion, null, null, null);
    }

    public static ResultadoDeCobro pendienteConQr(String idPagoExterno, String qrData, LocalDateTime fechaExpiracionQr) {
        return new ResultadoDeCobro(EstadoCobro.PENDIENTE, null, idPagoExterno, qrData, fechaExpiracionQr);
    }
}
