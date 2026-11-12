package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.entity.Disponibilidad;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaDeCanchaImplTest {

    // 2026-10-19 es lunes
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);

    @Mock
    private HorarioSemanalRepository horarioRepository;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;

    private AgendaDeCanchaImpl agenda;

    @BeforeEach
    void setUp() {
        agenda = new AgendaDeCanchaImpl(horarioRepository, disponibilidadRepository);
    }

    private static HorarioSemanal horario(DayOfWeek dia, int desde, int hasta) {
        HorarioSemanal horario = new HorarioSemanal();
        horario.setDiaSemana(dia);
        horario.setHoraInicio(LocalTime.of(desde, 0));
        horario.setHoraFin(LocalTime.of(hasta, 0));
        return horario;
    }

    private static FranjaHoraria franja(int desde, int hasta) {
        return new FranjaHoraria(LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private void horarios(HorarioSemanal... horarios) {
        when(horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(1L)).thenReturn(List.of(horarios));
    }

    @Test
    void dentroDeHorario_unTurnoQueCabeEnUnHorario() {
        horarios(horario(DayOfWeek.MONDAY, 8, 22));

        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(18, 20))).isTrue();
        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(8, 9))).isTrue();
        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(21, 22))).isTrue();
    }

    @Test
    void dentroDeHorario_unTurnoQueSeSaleDelHorario_false() {
        horarios(horario(DayOfWeek.MONDAY, 8, 22));

        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(7, 9))).isFalse();
        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(21, 23))).isFalse();
    }

    @Test
    void dentroDeHorario_unTurnoQueAtraviesaDosHorariosSeguidos_true() {
        horarios(horario(DayOfWeek.MONDAY, 8, 12), horario(DayOfWeek.MONDAY, 12, 14));

        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(11, 13))).isTrue();
    }

    @Test
    void dentroDeHorario_conUnHuecoEntreHorarios_false() {
        horarios(horario(DayOfWeek.MONDAY, 8, 12), horario(DayOfWeek.MONDAY, 14, 18));

        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(11, 15))).isFalse();
    }

    @Test
    void dentroDeHorario_enUnDiaSinHorario_false() {
        horarios(horario(DayOfWeek.TUESDAY, 8, 22));

        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(18, 20))).isFalse();
    }

    @Test
    void dentroDeHorario_sinHorariosCargados_false() {
        horarios();

        assertThat(agenda.dentroDeHorario(1L, LUNES, franja(18, 20))).isFalse();
    }

    private void bloqueos(Disponibilidad... bloqueos) {
        when(disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(1L, EstadoDisponibilidad.MANTENIMIENTO, LUNES, LUNES))
                .thenReturn(List.of(bloqueos));
    }

    private static Disponibilidad bloqueo(int desde, int hasta) {
        Disponibilidad bloqueo = new Disponibilidad();
        bloqueo.setHoraInicio(LocalTime.of(desde, 0));
        bloqueo.setHoraFin(LocalTime.of(hasta, 0));
        return bloqueo;
    }

    @Test
    void bloqueada_siElMantenimientoSePisaConElTurno_true_RD05() {
        bloqueos(bloqueo(8, 18));

        assertThat(agenda.bloqueada(1L, LUNES, franja(17, 19))).isTrue();
        assertThat(agenda.bloqueada(1L, LUNES, franja(9, 10))).isTrue();
    }

    @Test
    void bloqueada_siElTurnoEmpiezaJustoCuandoTerminaElMantenimiento_false() {
        bloqueos(bloqueo(8, 18));

        assertThat(agenda.bloqueada(1L, LUNES, franja(18, 20))).isFalse();
    }

    @Test
    void bloqueada_sinBloqueos_false() {
        bloqueos();

        assertThat(agenda.bloqueada(1L, LUNES, franja(18, 20))).isFalse();
    }
}
