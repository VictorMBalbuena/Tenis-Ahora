package com.tenisahora.modules.cancha.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Bloqueo por mantenimiento (CU091): todos los días del rango, en la franja horaria indicada. */
public record BloqueoRequest(
        @Schema(example = "2026-10-20") @NotNull LocalDate fechaDesde,
        @Schema(example = "2026-10-22") @NotNull LocalDate fechaHasta,
        @Schema(description = "Hora en punto", example = "08:00", type = "string") @NotNull LocalTime horaDesde,
        @Schema(description = "Hora en punto", example = "18:00", type = "string") @NotNull LocalTime horaHasta,
        @Schema(example = "Cambio de superficie") @NotBlank @Size(max = 255) String motivo
) {

    public BloqueoRequest {
        motivo = Textos.recortar(motivo);
    }
}
