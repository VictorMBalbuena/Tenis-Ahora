package com.tenisahora.modules.cancha.service;

import java.time.LocalDate;

/** Lo que otros módulos (reservas) necesitan saber de la agenda de una cancha: sus horarios semanales y sus bloqueos por mantenimiento. */
public interface AgendaDeCancha {

    /** true si cada hora de la franja cae dentro de un horario semanal de la cancha ese día de la semana. */
    boolean dentroDeHorario(Long canchaId, LocalDate fecha, FranjaHoraria franja);

    /** true si la franja se pisa con un bloqueo por mantenimiento de ese día (RD05). */
    boolean bloqueada(Long canchaId, LocalDate fecha, FranjaHoraria franja);
}
