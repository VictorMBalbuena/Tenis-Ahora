package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/** Modificación parcial (CU17): solo se cambian los campos que vienen. Los jugadores no se tocan. */
public record ReservaUpdateRequest(
        @Schema(example = "2") @Positive Long canchaId,
        @Schema(example = "2026-10-21") LocalDate fecha,
        @Schema(example = "19:00", type = "string") LocalTime horaInicio,
        @Schema(example = "1") @Min(1) @Max(12) Integer duracionHoras
) {
}
