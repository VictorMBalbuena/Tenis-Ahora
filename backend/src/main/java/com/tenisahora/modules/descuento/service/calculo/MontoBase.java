package com.tenisahora.modules.descuento.service.calculo;

import java.math.BigDecimal;

/** La punta de la cadena: el monto antes de cualquier descuento. */
public class MontoBase implements Calculable {

    private final BigDecimal monto;

    public MontoBase(BigDecimal monto) {
        this.monto = monto;
    }

    @Override
    public BigDecimal calcular() {
        return monto;
    }
}
