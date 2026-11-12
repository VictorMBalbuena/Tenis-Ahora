package com.tenisahora.modules.torneo.dto;

import jakarta.validation.constraints.NotNull;

public record InscribirTorneoRequest(
        @NotNull(message = "El jugador 1 es obligatorio") Long jugador1Id,
        Long parejaId,
        String nombreEquipo
) {
}
