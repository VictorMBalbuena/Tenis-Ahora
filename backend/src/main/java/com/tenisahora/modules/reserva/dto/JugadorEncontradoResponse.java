package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Lo mínimo para sumar a un socio como acompañante: no expone su contacto ni sus datos personales. */
public record JugadorEncontradoResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Luciana") String nombre,
        @Schema(example = "Fernández") String apellido
) {
}
