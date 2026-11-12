package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.service.AgendaDeCancha;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.cancha.service.ReservasDelRango;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Comprueba que una clase o un entrenamiento se puedan programar o modificar: cancha activa, dentro de su horario y
 * sin bloqueo en cada uno de sus días, y sin pisarse con otra actividad ni con una reserva ya tomada.
 */
@Component
@RequiredArgsConstructor
public class ValidadorDeAgendaDeActividades {

    private static final List<EstadoActividad> QUE_OCUPAN = EstadoActividad.queOcupanCancha();
    private static final Long NINGUNA = 0L;

    private final AgendaDeCancha agenda;
    private final ReservasDelRango reservasDelRango;
    private final ActividadRepository actividadRepository;

    /** excluirActividadId es la actividad que se está modificando (no se choca con sí misma); null si es una actividad nueva. */
    public void asegurarAgendaLibre(Cancha cancha, LocalDate fechaInicio, LocalDate fechaFin, Set<DayOfWeek> dias,
                                    FranjaHoraria franja, Long excluirActividadId) {
        if (!Boolean.TRUE.equals(cancha.getActiva())) {
            throw new ReglaDeNegocioException(CodigoError.CANCHA_INACTIVA, "La cancha está deshabilitada");
        }
        for (DayOfWeek dia : dias) {
            LocalDate representante = primeraFechaDesde(fechaInicio, dia);
            asegurarHorarioLibre(cancha.getId(), representante, franja);
        }
        Long excluir = excluirActividadId == null ? NINGUNA : excluirActividadId;
        if (actividadRepository.haySuperposicion(cancha.getId(), fechaInicio, fechaFin, dias, franja.desde(), franja.hasta(), QUE_OCUPAN, excluir)) {
            throw new ConflictoException(CodigoError.CANCHA_OCUPADA, "Ese horario ya lo ocupa otra clase o entrenamiento");
        }
        for (DayOfWeek dia : dias) {
            if (!reservasDelRango.idsVigentesEnElDia(cancha.getId(), fechaInicio, dia, franja).isEmpty()) {
                throw new ConflictoException(CodigoError.CANCHA_OCUPADA, "Ese horario ya tiene una reserva");
            }
        }
    }

    private void asegurarHorarioLibre(Long canchaId, LocalDate fecha, FranjaHoraria franja) {
        if (!agenda.dentroDeHorario(canchaId, fecha, franja)) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_FUERA_DE_HORARIO, "La cancha no atiende en ese día y horario");
        }
        if (agenda.bloqueada(canchaId, fecha, franja)) {
            throw new ConflictoException(CodigoError.RESERVA_HORARIO_BLOQUEADO, "La cancha está en mantenimiento en ese horario");
        }
    }

    /** La primera fecha desde "desde" (inclusive) que cae en ese día de la semana. */
    private LocalDate primeraFechaDesde(LocalDate desde, DayOfWeek dia) {
        int diferencia = dia.getValue() - desde.getDayOfWeek().getValue();
        return desde.plusDays(diferencia < 0 ? diferencia + 7 : diferencia);
    }
}
