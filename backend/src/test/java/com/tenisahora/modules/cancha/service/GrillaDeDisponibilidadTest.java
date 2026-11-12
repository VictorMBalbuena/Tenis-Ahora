package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.EstadoFranja;
import com.tenisahora.modules.cancha.dto.FranjaResponse;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrillaDeDisponibilidadTest {

    // 2026-10-19 es lunes
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final LocalDate MARTES = LUNES.plusDays(1);

    private static HorarioSemanal horario(DayOfWeek dia, int desde, int hasta) {
        HorarioSemanal horario = new HorarioSemanal();
        horario.setDiaSemana(dia);
        horario.setHoraInicio(LocalTime.of(desde, 0));
        horario.setHoraFin(LocalTime.of(hasta, 0));
        return horario;
    }

    private static FranjaDelDia franjaDelDia(LocalDate fecha, int desde, int hasta) {
        return new FranjaDelDia(fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private static List<EstadoFranja> estados(List<FranjaResponse> grilla) {
        return grilla.stream().map(FranjaResponse::estado).toList();
    }

    @Test
    void sinBloqueosNiReservas_todasLasHorasDelHorarioEstanLibres() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, LUNES,
                List.of(horario(DayOfWeek.MONDAY, 8, 11)), List.of(), List.of());

        assertThat(grilla).extracting(FranjaResponse::desde)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThat(grilla).extracting(FranjaResponse::hasta)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertThat(estados(grilla)).containsOnly(EstadoFranja.LIBRE);
    }

    @Test
    void unaHoraBloqueada_noFiguraComoLibre_RD05() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, LUNES,
                List.of(horario(DayOfWeek.MONDAY, 8, 11)), List.of(franjaDelDia(LUNES, 9, 10)), List.of());

        assertThat(estados(grilla)).containsExactly(EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA, EstadoFranja.LIBRE);
    }

    @Test
    void unaReserva_dejaLasHorasQueCubreComoOcupadas() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, LUNES,
                List.of(horario(DayOfWeek.MONDAY, 8, 12)), List.of(), List.of(franjaDelDia(LUNES, 9, 11)));

        assertThat(estados(grilla)).containsExactly(EstadoFranja.LIBRE, EstadoFranja.OCUPADA, EstadoFranja.OCUPADA, EstadoFranja.LIBRE);
    }

    @Test
    void distingueReservasYClasesConSuTipo() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, LUNES,
                List.of(horario(DayOfWeek.MONDAY, 8, 12)),
                List.of(franjaDelDia(LUNES, 8, 9)),
                List.of(franjaDelDia(LUNES, 9, 10)),
                List.of(franjaDelDia(LUNES, 10, 11)));

        assertThat(estados(grilla)).containsExactly(
                EstadoFranja.BLOQUEADA, EstadoFranja.OCUPADA, EstadoFranja.OCUPADA, EstadoFranja.LIBRE);
        assertThat(grilla).extracting(FranjaResponse::tipo).containsExactly(
                "MANTENIMIENTO", "RESERVA", "CLASE", null);
    }

    @Test
    void siHayBloqueoYReserva_mandaElBloqueo() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, LUNES,
                List.of(horario(DayOfWeek.MONDAY, 8, 9)), List.of(franjaDelDia(LUNES, 8, 9)), List.of(franjaDelDia(LUNES, 8, 9)));

        assertThat(estados(grilla)).containsExactly(EstadoFranja.BLOQUEADA);
    }

    @Test
    void unBloqueoOReservaDeOtroDia_noAfectaEsteDia() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, LUNES,
                List.of(horario(DayOfWeek.MONDAY, 8, 10)), List.of(franjaDelDia(MARTES, 8, 10)), List.of(franjaDelDia(MARTES, 8, 10)));

        assertThat(estados(grilla)).containsOnly(EstadoFranja.LIBRE);
    }

    @Test
    void unDiaSinHorarioSemanal_noTieneFranjas() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(MARTES, MARTES,
                List.of(horario(DayOfWeek.MONDAY, 8, 10)), List.of(), List.of());

        assertThat(grilla).isEmpty();
    }

    @Test
    void variosDias_salenOrdenadosPorFechaYHora() {
        List<FranjaResponse> grilla = GrillaDeDisponibilidad.calcular(LUNES, MARTES,
                List.of(horario(DayOfWeek.TUESDAY, 8, 9), horario(DayOfWeek.MONDAY, 14, 15), horario(DayOfWeek.MONDAY, 8, 9)),
                List.of(), List.of());

        assertThat(grilla).extracting(FranjaResponse::fecha, FranjaResponse::desde)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(LUNES, LocalTime.of(8, 0)),
                        org.assertj.core.groups.Tuple.tuple(LUNES, LocalTime.of(14, 0)),
                        org.assertj.core.groups.Tuple.tuple(MARTES, LocalTime.of(8, 0)));
    }

    @Test
    void sinHorarios_laGrillaVieneVacia() {
        assertThat(GrillaDeDisponibilidad.calcular(LUNES, MARTES, List.of(), List.of(), List.of())).isEmpty();
    }
}
