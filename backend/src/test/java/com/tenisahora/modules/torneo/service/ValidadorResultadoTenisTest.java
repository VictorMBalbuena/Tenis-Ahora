package com.tenisahora.modules.torneo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorResultadoTenisTest {

    @ParameterizedTest
    @CsvSource({
            "'6-3 6-4', 2, 0, 1",
            "'4-6 6-4 7-5', 2, 1, 1",
            "'3-6 2-6', 0, 2, 2",
            "'7-6 6-7 7-6', 2, 1, 1",
            "'6-0 6-0', 2, 0, 1",
            "'7-5 5-7 6-7', 1, 2, 2"
    })
    @DisplayName("Resultados válidos determinan ganador y sets correctamente")
    void resultadosValidos(String tanteador, int setsA, int setsB, int ganador) {
        ValidadorResultadoTenis.ResultadoValidado resultado = ValidadorResultadoTenis.validar(tanteador);

        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.setsGanadosA()).isEqualTo(setsA);
        assertThat(resultado.setsGanadosB()).isEqualTo(setsB);
        assertThat(resultado.ganador()).isEqualTo(ganador);
        assertThat(resultado.error()).isNull();
        assertThat(resultado.descripcion()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9-2",
            "6-5",
            "6-2 6-1 6-4",
            "4-4",
            "6-3",
            "6-4 4-6",
            "8-6",
            "7-4",
            "abc",
            "6-3-2",
            "  "
    })
    @DisplayName("Resultados con formato inválido o inconclusos son rechazados")
    void resultadosInvalidos(String tanteador) {
        ValidadorResultadoTenis.ResultadoValidado resultado = ValidadorResultadoTenis.validar(tanteador);

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.ganador()).isNull();
        assertThat(resultado.error()).isNotBlank();
    }

    @Test
    @DisplayName("Tanteador nulo es rechazado")
    void tanteadorNulo() {
        ValidadorResultadoTenis.ResultadoValidado resultado = ValidadorResultadoTenis.validar(null);

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.error()).isNotBlank();
    }
}
