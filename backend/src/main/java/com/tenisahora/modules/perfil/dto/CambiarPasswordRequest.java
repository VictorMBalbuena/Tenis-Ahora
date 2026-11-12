package com.tenisahora.modules.perfil.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /me/password. Pide la actual a propósito: si alguien se dejó la sesión abierta, que no pueda cambiarla. */
@Schema(description = "Cambio de la propia contraseña.")
public record CambiarPasswordRequest(
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @Schema(description = "Entre 8 y 72 caracteres, distinta de la actual", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String passwordNueva
) {
}
