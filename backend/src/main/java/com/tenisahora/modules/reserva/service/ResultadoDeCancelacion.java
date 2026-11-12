package com.tenisahora.modules.reserva.service;

import java.math.BigDecimal;

/** Qué corresponde al cancelar: si es a tiempo, el cargo que se aplica (RD12) y lo que se devuelve (RD13). */
public record ResultadoDeCancelacion(boolean aTiempo, long horasDeAntelacion, BigDecimal montoPenalidad, BigDecimal montoReembolso) {
}
