package com.tenisahora.modules.cancha.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueoResponse(
        @Schema(example = "1") Long canchaId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        @Schema(example = "08:00:00", type = "string") LocalTime horaDesde,
        @Schema(example = "18:00:00", type = "string") LocalTime horaHasta,
        String motivo,
        @Schema(description = "Cantidad de días bloqueados", example = "3") int dias
) {
}
