package com.tenisahora.modules.cancha.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/** Un día bloqueado por mantenimiento. Cada día se levanta por separado con su id. */
public record BloqueoDelDiaResponse(
        @Schema(example = "7") Long id,
        LocalDate fecha,
        @Schema(example = "08:00:00", type = "string") LocalTime desde,
        @Schema(example = "18:00:00", type = "string") LocalTime hasta,
        String motivo
) {
}
