package com.tenisahora.modules.insumo.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de una categoría de insumo nueva. */
public record CategoriaInsumoRequest(
        @Schema(description = "Nombre de la categoría; no puede repetirse", example = "Grip / overgrip")
        @NotBlank @Size(max = 50) String nombre
) {

    public CategoriaInsumoRequest {
        nombre = Textos.recortar(nombre);
    }
}
