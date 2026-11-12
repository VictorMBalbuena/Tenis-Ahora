package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Igual que en las reservas: con antelación se devuelve lo pagado; sobre la hora se pierde. Sin pagar no hay nada en juego. */
@Component
@RequiredArgsConstructor
public class PoliticaDeBajaDeInscripcion {

    public record ResultadoDeBaja(boolean aTiempo, BigDecimal montoPenalidad, BigDecimal montoReembolso) {
    }

    private final InscripcionesProperties propiedades;
    private final PagosDeInscripcion pagos;

    public ResultadoDeBaja evaluar(InscripcionClase inscripcion, LocalDateTime ahora) {
        Actividad actividad = inscripcion.getActividad();
        LocalDateTime inicio = actividad.getFechaInicio().atTime(actividad.getHoraInicio());
        boolean aTiempo = !ahora.plusHours(propiedades.plazoCancelacionHoras()).isAfter(inicio);
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            return new ResultadoDeBaja(aTiempo, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal cobrado = pagos.resumen(inscripcion).cobrado();
        if (!aTiempo) {
            return new ResultadoDeBaja(false, cobrado, BigDecimal.ZERO);
        }
        BigDecimal reembolso = cobrado.multiply(BigDecimal.valueOf(propiedades.porcentajeReembolsoATiempo()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ResultadoDeBaja(true, BigDecimal.ZERO, reembolso);
    }
}
