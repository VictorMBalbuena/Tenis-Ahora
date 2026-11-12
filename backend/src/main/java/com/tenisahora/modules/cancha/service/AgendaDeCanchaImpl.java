package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaDeCanchaImpl implements AgendaDeCancha {

    private final HorarioSemanalRepository horarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean dentroDeHorario(Long canchaId, LocalDate fecha, FranjaHoraria franja) {
        List<HorarioSemanal> delDia = horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(canchaId).stream()
                .filter(horario -> horario.getDiaSemana() == fecha.getDayOfWeek()).toList();
        for (LocalTime hora = franja.desde(); hora.isBefore(franja.hasta()); hora = hora.plusHours(1)) {
            if (!cubierta(delDia, hora)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean bloqueada(Long canchaId, LocalDate fecha, FranjaHoraria franja) {
        return disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(canchaId, EstadoDisponibilidad.MANTENIMIENTO, fecha, fecha)
                .stream()
                .anyMatch(bloqueo -> new FranjaHoraria(bloqueo.getHoraInicio(), bloqueo.getHoraFin()).seSuperponeCon(franja));
    }

    private boolean cubierta(List<HorarioSemanal> horarios, LocalTime hora) {
        return horarios.stream().anyMatch(horario -> !hora.isBefore(horario.getHoraInicio()) && hora.isBefore(horario.getHoraFin()));
    }
}
