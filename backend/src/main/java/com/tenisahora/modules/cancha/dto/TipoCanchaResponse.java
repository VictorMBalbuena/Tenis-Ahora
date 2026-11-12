package com.tenisahora.modules.cancha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Datos devueltos para un tipo de cancha. */
public record TipoCanchaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Polvo de Ladrillo Dobles") String nombre,
        @Schema(example = "Polvo de ladrillo (arcilla roja)") String superficie,
        @Schema(example = "4") Integer capacidad,
        @Schema(example = "Cancha de polvo de ladrillo para 4 jugadores") String descripcion,
        @Schema(example = "true") Boolean activo,
        @Schema(description = "Cantidad de canchas que utilizan este tipo", example = "3") int cantidadCanchas
) {
    @JsonProperty("tipoSuperficie")
    public String tipoSuperficie() {
        return superficie;
    }
}

