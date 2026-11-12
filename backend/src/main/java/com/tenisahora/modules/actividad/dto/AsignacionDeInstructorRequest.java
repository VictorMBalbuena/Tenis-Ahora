package com.tenisahora.modules.actividad.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Asignar un profesor o un entrenador a una actividad particular (CU44). */
public record AsignacionDeInstructorRequest(
        @Schema(example = "ENTRENADOR") @NotNull Rol rol,
        @Schema(example = "5") @NotNull @Positive Long instructorId
) {
}
