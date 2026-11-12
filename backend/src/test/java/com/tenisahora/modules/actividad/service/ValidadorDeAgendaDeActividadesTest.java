package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.service.AgendaDeCancha;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.cancha.service.ReservasDelRango;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static com.tenisahora.modules.actividad.DatosDePrueba.cancha;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorDeAgendaDeActividadesTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 2);
    private static final LocalDate HASTA = LocalDate.of(2027, 3, 2);
    private static final FranjaHoraria FRANJA = new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(19, 0));
    private static final Set<DayOfWeek> LUNES = Set.of(DayOfWeek.MONDAY);
    private static final List<EstadoActividad> OCUPAN = EstadoActividad.queOcupanCancha();

    @Mock
    private AgendaDeCancha agenda;
    @Mock
    private ReservasDelRango reservasDelRango;
    @Mock
    private ActividadRepository actividadRepository;

    private ValidadorDeAgendaDeActividades validador;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        validador = new ValidadorDeAgendaDeActividades(agenda, reservasDelRango, actividadRepository);
        cancha = cancha(1L);
    }

    private void agendaLibre() {
        when(agenda.dentroDeHorario(1L, LocalDate.of(2026, 11, 2), FRANJA)).thenReturn(true);
        when(agenda.bloqueada(1L, LocalDate.of(2026, 11, 2), FRANJA)).thenReturn(false);
    }

    @Test
    void agendaLibre_noLanzaNada() {
        agendaLibre();
        when(actividadRepository.haySuperposicion(1L, DESDE, HASTA, LUNES, FRANJA.desde(), FRANJA.hasta(), OCUPAN, 0L)).thenReturn(false);
        when(reservasDelRango.idsVigentesEnElDia(1L, DESDE, DayOfWeek.MONDAY, FRANJA)).thenReturn(List.of());

        validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, null);
    }

    @Test
    void canchaDeshabilitada_422() {
        cancha.setActiva(false);

        assertThatThrownBy(() -> validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CANCHA_INACTIVA");
        verify(agenda, never()).dentroDeHorario(anyLong(), any(), any());
    }

    @Test
    void fueraDeHorario_422() {
        when(agenda.dentroDeHorario(1L, LocalDate.of(2026, 11, 2), FRANJA)).thenReturn(false);

        assertThatThrownBy(() -> validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_FUERA_DE_HORARIO");
    }

    @Test
    void bloqueadaPorMantenimiento_409() {
        when(agenda.dentroDeHorario(1L, LocalDate.of(2026, 11, 2), FRANJA)).thenReturn(true);
        when(agenda.bloqueada(1L, LocalDate.of(2026, 11, 2), FRANJA)).thenReturn(true);

        assertThatThrownBy(() -> validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, null))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("RESERVA_HORARIO_BLOQUEADO");
    }

    @Test
    void solapadaConOtraActividad_409() {
        agendaLibre();
        when(actividadRepository.haySuperposicion(1L, DESDE, HASTA, LUNES, FRANJA.desde(), FRANJA.hasta(), OCUPAN, 0L)).thenReturn(true);

        assertThatThrownBy(() -> validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, null))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CANCHA_OCUPADA");
        verify(reservasDelRango, never()).idsVigentesEnElDia(any(), any(), any(), any());
    }

    @Test
    void solapadaConUnaReservaExistente_409() {
        agendaLibre();
        when(actividadRepository.haySuperposicion(1L, DESDE, HASTA, LUNES, FRANJA.desde(), FRANJA.hasta(), OCUPAN, 0L)).thenReturn(false);
        when(reservasDelRango.idsVigentesEnElDia(1L, DESDE, DayOfWeek.MONDAY, FRANJA)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, null))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CANCHA_OCUPADA");
    }

    @Test
    void alModificar_laPropiaActividadNoCuentaComoOcupacion() {
        agendaLibre();
        when(actividadRepository.haySuperposicion(eq(1L), eq(DESDE), eq(HASTA), eq(LUNES), eq(FRANJA.desde()), eq(FRANJA.hasta()),
                eq(OCUPAN), eq(55L))).thenReturn(false);
        when(reservasDelRango.idsVigentesEnElDia(1L, DESDE, DayOfWeek.MONDAY, FRANJA)).thenReturn(List.of());

        validador.asegurarAgendaLibre(cancha, DESDE, HASTA, LUNES, FRANJA, 55L);

        verify(actividadRepository).haySuperposicion(1L, DESDE, HASTA, LUNES, FRANJA.desde(), FRANJA.hasta(), OCUPAN, 55L);
    }
}
