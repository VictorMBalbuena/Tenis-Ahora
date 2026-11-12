package com.tenisahora.modules.cancha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/** Franja semanal de disponibilidad (CU090): los mismos horarios para cada uno de los días indicados. */
public record HorarioRequest(
        @Schema(description = "Días de la semana", example = "[\"MONDAY\", \"TUESDAY\"]")
        @NotEmpty @Size(max = 7) Set<@NotNull DayOfWeek> dias,
        @Schema(description = "Hora en punto desde la que está disponible", example = "08:00", type = "string")
        @NotNull LocalTime desde,
        @Schema(description = "Hora en punto hasta la que está disponible", example = "12:00", type = "string")
        @NotNull LocalTime hasta
) {
}
