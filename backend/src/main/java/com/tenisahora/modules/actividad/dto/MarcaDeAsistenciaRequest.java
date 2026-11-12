package com.tenisahora.modules.actividad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** La marca de un alumno dentro de un registro de asistencia (CU42, CU43). */
public record MarcaDeAsistenciaRequest(
        @Schema(example = "3") @NotNull @Positive Long socioId,
        @Schema(example = "true") @NotNull Boolean presente
) {
}
