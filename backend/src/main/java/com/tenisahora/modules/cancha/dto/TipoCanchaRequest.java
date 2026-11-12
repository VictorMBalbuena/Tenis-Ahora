package com.tenisahora.modules.cancha.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para registrar o modificar un tipo de cancha. */
public record TipoCanchaRequest(
        @Schema(description = "Nombre descriptivo del tipo de cancha", example = "Polvo de Ladrillo Dobles")
        @NotBlank @Size(max = 50) String nombre,
        @Schema(description = "Superficie o terminación del suelo", example = "Polvo de ladrillo (arcilla roja)")
        @NotBlank(message = "La superficie es obligatoria y no puede estar vacía")
        @Size(max = 100)
        @JsonAlias({"tipoSuperficie", "superficie"})
        String superficie,
        @Schema(description = "Capacidad de jugadores (2 o 4)", example = "4")
        @NotNull @Min(2) @Max(4) Integer capacidad,
        @Schema(description = "Descripción adicional opcional", example = "Cancha de polvo de ladrillo para 4 jugadores")
        @Size(max = 200) String descripcion
) {
    public TipoCanchaRequest {
        nombre = Textos.recortar(nombre);
        superficie = Textos.recortar(superficie);
        if (descripcion != null) {
            descripcion = descripcion.strip();
        }
    }

    public String tipoSuperficie() {
        return superficie;
    }
}

