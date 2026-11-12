package com.tenisahora.modules.cobro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionRequest(
        @Schema(example = "Se cargó a la reserva equivocada") @NotBlank(message = "Indicá el motivo") @Size(max = 255) String motivo
) {
}
