package com.tenisahora.modules.reserva.service;

import java.math.BigDecimal;

/** Lo que una reserva necesita saber de sus cobros, sin depender del módulo que los maneja. */
public interface PagosDeReserva {

    Resumen resumen(Long reservaId);

    /**
     * @param baseCubierta parte del total ya cubierta, antes de los descuentos
     * @param cobrado      lo que realmente entró: es el tope de cualquier reembolso
     */
    record Resumen(BigDecimal baseCubierta, BigDecimal cobrado) {
    }
}
