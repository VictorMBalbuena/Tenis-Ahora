package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.service.ActividadesDelRango;
import com.tenisahora.modules.cancha.service.AgendaDeCancha;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.cancha;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorDeAgendaTest {

    private static final LocalDate FECHA = HOY.plusDays(2);
    private static final FranjaHoraria FRANJA = new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(20, 0));
    private static final List<EstadoReserva> OCUPAN = EstadoReserva.queRetienenHorario();

    @Mock
    private AgendaDeCancha agenda;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ActividadesDelRango actividadesDelRango;

    private ValidadorDeAgenda validador;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        validador = new ValidadorDeAgenda(agenda, reservaRepository, actividadesDelRango);
        cancha = cancha(1L, "Cancha 1", "8000");
    }

    private void agendaLibre() {
        when(agenda.dentroDeHorario(1L, FECHA, FRANJA)).thenReturn(true);
        when(agenda.bloqueada(1L, FECHA, FRANJA)).thenReturn(false);
    }

    @Test
    void turnoLibre_noLanzaNada_yUnaReservaNuevaNoExcluyeNinguna() {
        agendaLibre();
        when(reservaRepository.haySuperposicion(1L, OCUPAN, FECHA, FRANJA.desde(), FRANJA.hasta(), 0L)).thenReturn(false);

        validador.asegurarTurnoLibre(cancha, FECHA, FRANJA, null);

        verify(reservaRepository).haySuperposicion(1L, OCUPAN, FECHA, FRANJA.desde(), FRANJA.hasta(), 0L);
    }

    @Test
    void alModificar_laPropiaReservaNoCuentaComoOcupacion() {
        agendaLibre();
        when(reservaRepository.haySuperposicion(1L, OCUPAN, FECHA, FRANJA.desde(), FRANJA.hasta(), 55L)).thenReturn(false);

        validador.asegurarTurnoLibre(cancha, FECHA, FRANJA, 55L);

        verify(reservaRepository).haySuperposicion(1L, OCUPAN, FECHA, FRANJA.desde(), FRANJA.hasta(), 55L);
    }

    @Test
    void canchaDeshabilitada_422() {
        cancha.setActiva(false);

        assertThatThrownBy(() -> validador.asegurarTurnoLibre(cancha, FECHA, FRANJA, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CANCHA_INACTIVA");
        verify(agenda, never()).dentroDeHorario(anyLong(), any(), any());
    }

    @Test
    void fueraDelHorarioDeLaCancha_422() {
        when(agenda.dentroDeHorario(1L, FECHA, FRANJA)).thenReturn(false);

        assertThatThrownBy(() -> validador.asegurarTurnoLibre(cancha, FECHA, FRANJA, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_FUERA_DE_HORARIO");
    }

    @Test
    void conBloqueoPorMantenimiento_409_RD05() {
        when(agenda.dentroDeHorario(1L, FECHA, FRANJA)).thenReturn(true);
        when(agenda.bloqueada(1L, FECHA, FRANJA)).thenReturn(true);

        assertThatThrownBy(() -> validador.asegurarTurnoLibre(cancha, FECHA, FRANJA, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("mantenimiento")
                .extracting("codigo").isEqualTo("RESERVA_HORARIO_BLOQUEADO");
        verify(reservaRepository, never()).haySuperposicion(any(), any(), any(), any(), any(), any());
    }

    @Test
    void conOtraReservaEncima_409() {
        agendaLibre();
        when(reservaRepository.haySuperposicion(1L, OCUPAN, FECHA, FRANJA.desde(), FRANJA.hasta(), 0L)).thenReturn(true);

        assertThatThrownBy(() -> validador.asegurarTurnoLibre(cancha, FECHA, FRANJA, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya está reservado")
                .extracting("codigo").isEqualTo("RESERVA_HORARIO_OCUPADO");
    }
}
