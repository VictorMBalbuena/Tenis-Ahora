package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Lo que pasaría al cancelar ahora, sin cambiar nada: sirve para avisar antes de confirmar (CU18 A1). */
public record CancelacionResponse(
        @Schema(description = "true si faltan el plazo mínimo o más") boolean aTiempo,
        @Schema(example = "30") long horasDeAntelacion,
        BigDecimal montoPenalidad,
        BigDecimal montoReembolso,
        @Schema(example = "Cancelás a tiempo: corresponde el reembolso") String mensaje
) {
}
