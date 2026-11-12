package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record JugadorResponse(
        @Schema(description = "Vacío si es un invitado", example = "3") Long personaId,
        String nombre,
        String apellido,
        String documento,
        boolean titular,
        boolean invitado
) {
}
