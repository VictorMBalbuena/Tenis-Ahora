package com.tenisahora.modules.actividad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/** Inscripción a una clase (CU80). Sin socioId, el socio se inscribe a sí mismo; con socioId, solo lo puede indicar el personal. */
public record InscripcionRequest(
        @Schema(description = "Solo lo indica un empleado: el socio a inscribir. Un socio se inscribe siempre a sí mismo", example = "3")
        @Positive Long socioId
) {
}
