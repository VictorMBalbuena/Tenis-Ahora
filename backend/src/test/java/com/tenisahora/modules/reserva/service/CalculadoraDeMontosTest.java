package com.tenisahora.modules.reserva.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;

/** El total y la seña de una reserva (RD09), con la regla por defecto: seña del 50%. */
class CalculadoraDeMontosTest {

    private final CalculadoraDeMontos calculadora = new CalculadoraDeMontos(PROPIEDADES);

    // ---------- Montos ----------

    @Test
    void total_esElPrecioPorLasHoras() {
        assertThat(calculadora.total(new BigDecimal("8000"), 2)).isEqualByComparingTo("16000.00");
        assertThat(calculadora.total(new BigDecimal("6500.50"), 1)).isEqualByComparingTo("6500.50");
    }

    @Test
    void senia_esElCincuentaPorCiento_RD09() {
        assertThat(calculadora.senia(new BigDecimal("16000.00"))).isEqualByComparingTo("8000.00");
    }

    @Test
    void porcentaje_redondeaHaciaArribaALosCentavos() {
        assertThat(calculadora.senia(new BigDecimal("100.01"))).isEqualByComparingTo("50.01");
    }
}
