package com.tenisahora.modules.insumo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategoriaInsumoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Raqueta") String nombre
) {
}
