package com.tenisahora.modules.cancha.service;

import java.time.LocalDate;
import java.time.LocalTime;

/** Una franja horaria de una fecha concreta: un bloqueo o una reserva de la cancha. */
public record FranjaDelDia(LocalDate fecha, LocalTime desde, LocalTime hasta) {

    public boolean seSuperponeCon(LocalDate otraFecha, FranjaHoraria franja) {
        return fecha.equals(otraFecha) && new FranjaHoraria(desde, hasta).seSuperponeCon(franja);
    }
}
