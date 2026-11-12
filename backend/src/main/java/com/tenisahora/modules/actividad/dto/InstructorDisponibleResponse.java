package com.tenisahora.modules.actividad.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

/** Un profesor o entrenador validado y libre en el horario de la actividad particular (CU44). */
public record InstructorDisponibleResponse(
        @Schema(example = "5") Long id,
        @Schema(example = "Pérez, Juan") String nombre,
        @Schema(example = "ENTRENADOR") Rol rol
) {
}
