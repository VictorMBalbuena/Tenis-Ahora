package com.tenisahora.modules.cancha.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Puerto: lo que el módulo de canchas necesita saber de las clases y entrenamientos. Lo implementa el módulo
 * actividad, así canchas no depende de él (las actividades ya dependen de las canchas). Espejo de {@link ReservasDelRango}.
 */
public interface ActividadesDelRango {

    /** Franjas que ocupan las actividades PROGRAMADA o EN_CURSO, entre las dos fechas inclusive. */
    List<FranjaDelDia> ocupadasEn(Long canchaId, LocalDate desde, LocalDate hasta);

    /** Ids de las actividades PROGRAMADA o EN_CURSO que, en esa fecha, caen en ese día de la semana y se pisan con la franja. */
    List<Long> idsVigentesEnElDia(Long canchaId, LocalDate fecha, DayOfWeek dia, FranjaHoraria franja);
}
