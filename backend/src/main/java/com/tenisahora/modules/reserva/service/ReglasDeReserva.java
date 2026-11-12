package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/** Las reglas de tiempo y de cantidad de las reservas (RD06, RD07, RD17). Sin base de datos: se prueban con un reloj falso. */
@Component
@RequiredArgsConstructor
public class ReglasDeReserva {

    private static final Set<Integer> CANTIDADES_DE_ACOMPANIANTES = Set.of(1, 3);

    private final ReservasProperties propiedades;
    private final Clock clock;

    /** Valida fecha, hora y duración del turno y devuelve la franja que ocupa. */
    public FranjaHoraria validarTurno(LocalDate fecha, LocalTime horaInicio, int duracionHoras) {
        FranjaHoraria franja = armarFranja(horaInicio, duracionHoras);
        if (!LocalDateTime.of(fecha, horaInicio).isAfter(LocalDateTime.now(clock))) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_FECHA_PASADA, "El turno tiene que empezar en el futuro");
        }
        if (!esReservable(fecha)) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_FUERA_DE_PLAZO,
                    "Se puede reservar hasta con " + propiedades.antelacionMaximaDias() + " días de antelación");
        }
        return franja;
    }

    /** RD06: desde hoy hasta hoy más la antelación máxima. */
    public boolean esReservable(LocalDate fecha) {
        LocalDate hoy = LocalDate.now(clock);
        return !fecha.isBefore(hoy) && !fecha.isAfter(hoy.plusDays(propiedades.antelacionMaximaDias()));
    }

    /** RD07: el titular juega siempre, así que con 1 o 3 acompañantes los jugadores son 2 o 4. */
    public void validarAcompaniantes(int cantidad) {
        if (!CANTIDADES_DE_ACOMPANIANTES.contains(cantidad)) {
            throw new SolicitudInvalidaException(CodigoError.RESERVA_JUGADORES_INVALIDOS,
                    "Una reserva lleva 2 o 4 jugadores: el titular y 1 o 3 acompañantes");
        }
    }

    private FranjaHoraria armarFranja(LocalTime horaInicio, int duracionHoras) {
        if (duracionHoras < 1 || duracionHoras > propiedades.duracionMaximaHoras()) {
            throw new SolicitudInvalidaException(CodigoError.RESERVA_DURACION_INVALIDA,
                    "La duración es de 1 a " + propiedades.duracionMaximaHoras() + " horas");
        }
        LocalTime horaFin = horaInicio.plusHours(duracionHoras);
        if (!horaFin.isAfter(horaInicio)) {
            throw new SolicitudInvalidaException(CodigoError.RESERVA_DURACION_INVALIDA, "El turno no puede pasar de medianoche");
        }
        return FranjaHoraria.validada(horaInicio, horaFin, CodigoError.RESERVA_DURACION_INVALIDA);
    }
}
