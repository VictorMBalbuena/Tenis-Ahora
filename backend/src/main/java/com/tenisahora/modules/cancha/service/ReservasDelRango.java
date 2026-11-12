package com.tenisahora.modules.cancha.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Puerto: lo que el módulo de canchas necesita saber de las reservas. Lo implementa el módulo reserva, así canchas no
 * depende de él (las reservas ya dependen de las canchas).
 */
public interface ReservasDelRango {

    /** Ids de las reservas CONFIRMADAS o pendientes de pago que se pisan con la franja diaria, entre las dos fechas inclusive. */
    List<Long> idsVigentesEn(Long canchaId, LocalDate desde, LocalDate hasta, FranjaHoraria franja);

    /** Franjas que ocupan las reservas CONFIRMADAS o pendientes de pago, entre las dos fechas inclusive. */
    List<FranjaDelDia> ocupadasEn(Long canchaId, LocalDate desde, LocalDate hasta);

    /**
     * Ids de las reservas CONFIRMADAS o pendientes de pago, desde la fecha en adelante, que caen en ese día de la semana
     * y se pisan con la franja. Sirve para saber si un horario semanal todavía se está usando.
     */
    List<Long> idsVigentesEnElDia(Long canchaId, LocalDate desde, DayOfWeek dia, FranjaHoraria franja);
}
