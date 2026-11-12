package com.tenisahora.shared.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos mínimos de una persona para los listados")
public record PersonaResumenResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "Ana") String nombre,
        @Schema(example = "Gómez") String apellido,
        @Schema(example = "30123456") String dni,
        @Schema(example = "ana@correo.com") String email,
        @Schema(example = "SOCIO") Rol rol,
        @Schema(description = "Si la cuenta puede iniciar sesión", example = "true") Boolean activo
) {
}
