package com.tenisahora.modules.cobro.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que cobro necesita del módulo de descuentos (RD21). Puerto invertido: {@code descuento} ya importa a
 * {@code cobro} (sus {@code CobroDescuento} referencian un {@code Cobro}), así que esta interfaz se declara acá,
 * con tipos neutros, y la implementa {@code modules.descuento} — igual que {@code UsoDeInsumos} o
 * {@code ReservasDelRango}. Evita el ciclo entre módulos que rompería {@code ArquitecturaTest}.
 */
public interface CalculoDeDescuentos {

    /**
     * Calcula el monto final aplicando los descuentos vigentes del socio sobre el monto base, y deja registrados
     * los descuentos aplicados. Sin descuentos cargados o sin socio, el monto final es igual al base.
     */
    ResultadoDeDescuento calcular(Long cobroId, BigDecimal montoBase, Long socioId);

    /** @param montoFinal lo que efectivamente se cobra: nunca negativo ni mayor al monto base */
    record ResultadoDeDescuento(BigDecimal montoFinal, List<String> codigosAplicados) {
    }
}
