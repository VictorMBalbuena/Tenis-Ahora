package com.tenisahora.modules.torneo.dto;

import jakarta.validation.constraints.NotBlank;

public record CargarResultadoRequest(
        @NotBlank(message = "El tanteador es obligatorio") String sets
) {
}
