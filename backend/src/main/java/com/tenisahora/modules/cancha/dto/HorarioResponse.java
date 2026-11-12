package com.tenisahora.modules.cancha.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioResponse(
        @Schema(example = "1") Long id,
        DayOfWeek dia,
        @Schema(example = "08:00:00", type = "string") LocalTime desde,
        @Schema(example = "12:00:00", type = "string") LocalTime hasta
) {
}
