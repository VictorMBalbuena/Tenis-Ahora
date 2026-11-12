package com.tenisahora.modules.reserva.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** El total y la seña los calcula siempre el servidor (RD09); el cliente nunca manda un monto. */
@Component
@RequiredArgsConstructor
public class CalculadoraDeMontos {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ReservasProperties propiedades;

    public BigDecimal total(BigDecimal precioPorHora, int horas) {
        return precioPorHora.multiply(BigDecimal.valueOf(horas)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal senia(BigDecimal total) {
        return porcentaje(total, propiedades.porcentajeSenia());
    }

    public BigDecimal porcentaje(BigDecimal monto, int porcentaje) {
        return monto.multiply(BigDecimal.valueOf(porcentaje)).divide(CIEN, 2, RoundingMode.HALF_UP);
    }
}
