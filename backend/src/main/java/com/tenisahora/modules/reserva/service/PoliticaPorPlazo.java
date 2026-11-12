package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Política del club: a tiempo (con el plazo de antelación o más) se devuelve el porcentaje configurado de lo pagado; fuera
 * de plazo se pierde la seña. Una reserva todavía sin pagar no tiene nada que devolver ni que perder.
 */
@Component
@RequiredArgsConstructor
public class PoliticaPorPlazo implements PoliticaDeCancelacion {

    private final ReservasProperties propiedades;
    private final CalculadoraDeMontos calculadora;
    private final PagosDeReserva pagosDeReserva;

    @Override
    public ResultadoDeCancelacion evaluar(Reserva reserva, LocalDateTime ahora) {
        Duration antelacion = Duration.between(ahora, reserva.getFecha().atTime(reserva.getHoraInicio()));
        boolean aTiempo = !antelacion.minus(Duration.ofHours(propiedades.plazoCancelacionHoras())).isNegative();
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            return new ResultadoDeCancelacion(aTiempo, antelacion.toHours(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal cobrado = pagosDeReserva.resumen(reserva.getId()).cobrado();
        return aTiempo
                ? new ResultadoDeCancelacion(true, antelacion.toHours(), BigDecimal.ZERO,
                        calculadora.porcentaje(cobrado, propiedades.porcentajeReembolsoATiempo()))
                : new ResultadoDeCancelacion(false, antelacion.toHours(), cobrado, BigDecimal.ZERO);
    }
}
