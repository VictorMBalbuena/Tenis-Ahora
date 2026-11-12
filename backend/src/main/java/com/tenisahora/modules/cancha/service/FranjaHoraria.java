package com.tenisahora.modules.cancha.service;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;

import java.time.LocalTime;

/** Rango de horas de un mismo día. Siempre en horas en punto y con el inicio antes del fin. */
public record FranjaHoraria(LocalTime desde, LocalTime hasta) {

    /** Arma la franja o rechaza el pedido con el código indicado (HORARIO_INVALIDO o BLOQUEO_INVALIDO). */
    public static FranjaHoraria validada(LocalTime desde, LocalTime hasta, CodigoError codigo) {
        if (!esHoraEnPunto(desde) || !esHoraEnPunto(hasta)) {
            throw new SolicitudInvalidaException(codigo, "Las horas deben ser en punto, por ejemplo 08:00");
        }
        return sinRestriccionDeMinuto(desde, hasta, codigo);
    }

    /** Igual que {@link #validada}, pero sin exigir que las horas sean en punto (clases y entrenamientos). */
    public static FranjaHoraria sinRestriccionDeMinuto(LocalTime desde, LocalTime hasta, CodigoError codigo) {
        if (!desde.isBefore(hasta)) {
            throw new SolicitudInvalidaException(codigo, "La hora de inicio debe ser anterior a la de fin");
        }
        return new FranjaHoraria(desde, hasta);
    }

    /** Se pisan si cada una empieza antes de que termine la otra: 8-10 y 10-12 no se pisan. */
    public boolean seSuperponeCon(FranjaHoraria otra) {
        return desde.isBefore(otra.hasta) && hasta.isAfter(otra.desde);
    }

    private static boolean esHoraEnPunto(LocalTime hora) {
        return hora.equals(hora.withMinute(0).withSecond(0).withNano(0));
    }
}
