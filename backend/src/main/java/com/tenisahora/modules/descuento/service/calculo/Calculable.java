package com.tenisahora.modules.descuento.service.calculo;

import java.math.BigDecimal;

/** Patrón DECORATOR: cada paso del cálculo envuelve al anterior y devuelve un monto (RD21). */
public interface Calculable {

    BigDecimal calcular();
}
