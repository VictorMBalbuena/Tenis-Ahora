package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaDelDia;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservasDelRangoImplTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    @Mock
    private ReservaRepository reservaRepository;
    @InjectMocks
    private ReservasDelRangoImpl adaptador;

    @Test
    void idsVigentesEn_pideLasConfirmadasYPendientesDePagoEnLaFranja() {
        FranjaHoraria franja = new FranjaHoraria(LocalTime.of(8, 0), LocalTime.of(18, 0));
        when(reservaRepository.idsPorEstadosEnRango(1L, EstadoReserva.queRetienenHorario(), DIA, DIA.plusDays(2), LocalTime.of(8, 0), LocalTime.of(18, 0)))
                .thenReturn(List.of(3L, 5L));

        assertThat(adaptador.idsVigentesEn(1L, DIA, DIA.plusDays(2), franja)).containsExactly(3L, 5L);
    }

    @Test
    void ocupadasEn_incluyeConfirmadasYPendientesDePago_yLasConvierteEnFranjas() {
        Reserva reserva = new Reserva();
        reserva.setFecha(DIA);
        reserva.setHoraInicio(LocalTime.of(9, 0));
        reserva.setHoraFin(LocalTime.of(11, 0));
        when(reservaRepository.findByCanchaIdAndEstadoInAndFechaBetween(1L,
                EstadoReserva.queRetienenHorario(), DIA, DIA)).thenReturn(List.of(reserva));

        assertThat(adaptador.ocupadasEn(1L, DIA, DIA))
                .containsExactly(new FranjaDelDia(DIA, LocalTime.of(9, 0), LocalTime.of(11, 0)));
    }

    @Test
    void idsVigentesEnElDia_quedaSoloConLasReservasDeEseDiaDeLaSemana() {
        // 2026-10-20 es martes y 2026-10-21 miércoles
        Reserva martes = reserva(4L, LocalDate.of(2026, 10, 20));
        Reserva miercoles = reserva(6L, LocalDate.of(2026, 10, 21));
        Reserva otroMartes = reserva(9L, LocalDate.of(2026, 10, 27));
        when(reservaRepository.vigentesDesde(1L, EstadoReserva.queRetienenHorario(),
                LocalDate.of(2026, 10, 19), LocalTime.of(8, 0), LocalTime.of(12, 0))).thenReturn(List.of(martes, miercoles, otroMartes));

        assertThat(adaptador.idsVigentesEnElDia(1L, LocalDate.of(2026, 10, 19), java.time.DayOfWeek.TUESDAY,
                new FranjaHoraria(LocalTime.of(8, 0), LocalTime.of(12, 0)))).containsExactly(4L, 9L);
    }

    private static Reserva reserva(Long id, LocalDate fecha) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setFecha(fecha);
        return reserva;
    }
}
