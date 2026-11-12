package com.tenisahora.modules.torneo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** El calendario de una liga: todos contra todos, una sola vez. */
class CalendarioDeLigaTest {

    @Test
    void conDosParticipantesHayUnSoloPartido() {
        List<CalendarioDeLiga.Cruce> cruces = CalendarioDeLiga.armar(2);

        assertThat(cruces).hasSize(1);
        assertThat(cruces.get(0).fecha()).isEqualTo(1);
    }

    @Test
    void conMenosDeDosNoHayLiga() {
        assertThat(CalendarioDeLiga.armar(1)).isEmpty();
        assertThat(CalendarioDeLiga.armar(0)).isEmpty();
        assertThat(CalendarioDeLiga.partidos(1)).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 12, 15, 16})
    void cadaUnoJuegaUnaVezContraCadaOtro(int cantidad) {
        List<CalendarioDeLiga.Cruce> cruces = CalendarioDeLiga.armar(cantidad);

        Set<String> parejas = cruces.stream().map(CalendarioDeLigaTest::pareja).collect(Collectors.toSet());

        // Ni se repite ningún cruce ni falta ninguno: n*(n-1)/2
        assertThat(parejas).hasSize(cruces.size());
        assertThat(cruces).hasSize(cantidad * (cantidad - 1) / 2);
        assertThat(CalendarioDeLiga.partidos(cantidad)).isEqualTo(cruces.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 12, 15, 16})
    void nadieJuegaDosVecesEnLaMismaFecha(int cantidad) {
        List<CalendarioDeLiga.Cruce> cruces = CalendarioDeLiga.armar(cantidad);

        cruces.stream().map(CalendarioDeLiga.Cruce::fecha).distinct().forEach(fecha -> {
            List<Integer> queJuegan = cruces.stream()
                    .filter(c -> c.fecha() == fecha)
                    .flatMap(c -> java.util.stream.Stream.of(c.localIndice(), c.visitanteIndice()))
                    .toList();
            assertThat(new HashSet<>(queJuegan)).hasSameSizeAs(queJuegan);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 8, 16})
    void conCantidadParNadieDescansa(int cantidad) {
        List<CalendarioDeLiga.Cruce> cruces = CalendarioDeLiga.armar(cantidad);

        int fechas = CalendarioDeLiga.fechas(cantidad);
        assertThat(fechas).isEqualTo(cantidad - 1);
        // En cada fecha juegan todos: la mitad de los participantes son partidos
        assertThat(cruces).hasSize(fechas * (cantidad / 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9, 15})
    void conCantidadImparDescansaUnoPorFecha(int cantidad) {
        List<CalendarioDeLiga.Cruce> cruces = CalendarioDeLiga.armar(cantidad);

        int fechas = CalendarioDeLiga.fechas(cantidad);
        assertThat(fechas).isEqualTo(cantidad);
        // Queda uno afuera en cada fecha, así que hay (n-1)/2 partidos por fecha
        assertThat(cruces).hasSize(fechas * ((cantidad - 1) / 2));
    }

    @Test
    void elOrdenDentroDeCadaFechaEmpiezaEnUnoYNoSeSaltea() {
        List<CalendarioDeLiga.Cruce> cruces = CalendarioDeLiga.armar(7);

        cruces.stream().map(CalendarioDeLiga.Cruce::fecha).distinct().forEach(fecha -> {
            List<Integer> ordenes = cruces.stream()
                    .filter(c -> c.fecha() == fecha)
                    .map(CalendarioDeLiga.Cruce::orden)
                    .sorted()
                    .toList();
            assertThat(ordenes.get(0)).isEqualTo(1);
            assertThat(ordenes.get(ordenes.size() - 1)).isEqualTo(ordenes.size());
        });
    }

    @Test
    void nadieJuegaContraSiMismo() {
        CalendarioDeLiga.armar(9).forEach(cruce ->
                assertThat(cruce.localIndice()).isNotEqualTo(cruce.visitanteIndice()));
    }

    /** El cruce A-B y el B-A son el mismo partido. */
    private static String pareja(CalendarioDeLiga.Cruce cruce) {
        int menor = Math.min(cruce.localIndice(), cruce.visitanteIndice());
        int mayor = Math.max(cruce.localIndice(), cruce.visitanteIndice());
        return menor + "-" + mayor;
    }
}
