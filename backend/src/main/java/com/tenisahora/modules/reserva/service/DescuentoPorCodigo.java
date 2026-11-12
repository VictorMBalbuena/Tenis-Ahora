package com.tenisahora.modules.reserva.service;

import java.math.BigDecimal;

/**
 * Lo que reserva necesita del módulo de descuentos para aplicar un código (CU62 a CU66).
 *
 * <p>Puerto invertido, igual que {@code UsoDeInsumos} o {@code CalculoDeDescuentos}: {@code descuento} ya
 * depende de {@code cobro}, y {@code cobro} de {@code reserva}. Si {@code reserva} importara {@code descuento}
 * se cerraría el ciclo y {@code ArquitecturaTest} lo rechazaría. Por eso la interfaz vive acá, con tipos
 * neutros, y la implementa {@code modules.descuento}.
 */
public interface DescuentoPorCodigo {

    /**
     * Qué descuento corresponde a ese código sobre ese monto.
     *
     * @param codigo lo que escribió la persona; se normaliza adentro, no hace falta mandarlo en mayúsculas
     * @param montoBase el total sobre el que se calcula
     * @return el descuento que corresponde aplicar
     * @throws com.tenisahora.shared.exception.RecursoNoEncontradoException si el código no existe o está inactivo
     */
    DescuentoAplicado aplicar(String codigo, BigDecimal montoBase);

    /**
     * Un descuento ya resuelto contra un monto.
     *
     * @param codigo el código tal como quedó guardado, en mayúsculas
     * @param descripcion cómo se le explica a la persona, por ejemplo "25% de descuento"
     * @param montoDescontado cuánto se resta; nunca mayor al monto base
     * @param montoFinal lo que queda a pagar; nunca negativo
     */
    record DescuentoAplicado(String codigo, String descripcion, BigDecimal montoDescontado, BigDecimal montoFinal) {
    }
}
