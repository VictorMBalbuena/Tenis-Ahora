package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.reserva.service.CalculadoraDeMontos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculadoraDeCobroTest {

    @Mock
    private CalculoDeDescuentos descuentos;

    private CalculadoraDeCobro calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraDeCobro(new CalculadoraDeMontos(com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES), descuentos);
    }

    @Test
    void montoBase_esLaSeniaDel50PorCiento() {
        assertThat(calculadora.montoBase(new BigDecimal("16000.00"))).isEqualByComparingTo("8000.00");
    }

    @Test
    void montoBase_redondeaHalfUpConTotalImpar() {
        assertThat(calculadora.montoBase(new BigDecimal("15001.00"))).isEqualByComparingTo("7500.50");
    }

    @Test
    void aplicarDescuentos_sinDescuentos_elFinalEsIgualAlBase() {
        when(descuentos.calcular(7L, new BigDecimal("8000.00"), 3L))
                .thenReturn(new CalculoDeDescuentos.ResultadoDeDescuento(new BigDecimal("8000.00"), List.of()));

        CalculadoraDeCobro.MontoDeCobro resultado = calculadora.aplicarDescuentos(7L, new BigDecimal("8000.00"), 3L);

        assertThat(resultado.montoBase()).isEqualByComparingTo(resultado.montoFinal());
        assertThat(resultado.descuentosAplicados()).isEmpty();
    }

    @Test
    void aplicarDescuentos_conDescuentoAplicado_elFinalEsMenor() {
        when(descuentos.calcular(7L, new BigDecimal("8000.00"), 3L))
                .thenReturn(new CalculoDeDescuentos.ResultadoDeDescuento(new BigDecimal("7200.00"), List.of("LIGA_REGULAR")));

        CalculadoraDeCobro.MontoDeCobro resultado = calculadora.aplicarDescuentos(7L, new BigDecimal("8000.00"), 3L);

        assertThat(resultado.montoFinal()).isEqualByComparingTo("7200.00");
        assertThat(resultado.descuentosAplicados()).containsExactly("LIGA_REGULAR");
    }
}
