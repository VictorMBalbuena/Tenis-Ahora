package com.tenisahora.modules.descuento.service.calculo;

import com.tenisahora.modules.descuento.entity.Descuento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aplica un descuento sobre lo que ya calculó la cadena anterior: por porcentaje o por monto fijo, nunca por debajo de cero. */
public class DescuentoDecorator implements Calculable {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final Calculable anterior;
    private final Descuento descuento;

    public DescuentoDecorator(Calculable anterior, Descuento descuento) {
        this.anterior = anterior;
        this.descuento = descuento;
    }

    @Override
    public BigDecimal calcular() {
        BigDecimal monto = anterior.calcular();
        BigDecimal reduccion = descuento.getPorcentaje() != null
                ? monto.multiply(descuento.getPorcentaje()).divide(CIEN, 2, RoundingMode.HALF_UP)
                : descuento.getMontoFijo();
        return monto.subtract(reduccion).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
