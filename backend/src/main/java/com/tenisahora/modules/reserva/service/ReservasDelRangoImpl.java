package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaDelDia;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.cancha.service.ReservasDelRango;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Lo que el módulo de canchas pregunta de las reservas. Una reserva pendiente de pago o en curso también retiene el horario. */
@Service
@RequiredArgsConstructor
public class ReservasDelRangoImpl implements ReservasDelRango {

    private static final List<EstadoReserva> ESTADOS_QUE_OCUPAN = EstadoReserva.queRetienenHorario();

    private final ReservaRepository reservaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsVigentesEn(Long canchaId, LocalDate desde, LocalDate hasta, FranjaHoraria franja) {
        return reservaRepository.idsPorEstadosEnRango(canchaId, ESTADOS_QUE_OCUPAN, desde, hasta, franja.desde(), franja.hasta());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FranjaDelDia> ocupadasEn(Long canchaId, LocalDate desde, LocalDate hasta) {
        return reservaRepository.findByCanchaIdAndEstadoInAndFechaBetween(canchaId, ESTADOS_QUE_OCUPAN, desde, hasta).stream()
                .map(reserva -> new FranjaDelDia(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsVigentesEnElDia(Long canchaId, LocalDate desde, DayOfWeek dia, FranjaHoraria franja) {
        return reservaRepository.vigentesDesde(canchaId, ESTADOS_QUE_OCUPAN, desde, franja.desde(), franja.hasta()).stream()
                .filter(reserva -> reserva.getFecha().getDayOfWeek() == dia)
                .map(Reserva::getId).toList();
    }
}
