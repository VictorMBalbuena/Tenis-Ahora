package com.tenisahora.modules.actividad.dto;

import java.math.BigDecimal;

/** Qué pasaría si se da de baja ahora: lo muestra la pantalla antes de confirmar. */
public record BajaDeInscripcionResponse(boolean aTiempo, BigDecimal montoPenalidad, BigDecimal montoReembolso, String mensaje) {
}
