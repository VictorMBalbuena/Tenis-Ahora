package com.tenisahora.modules.insumo.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos para registrar un insumo (CU10). No trae id ni estado de baja: nace activo. */
public record InsumoRequest(
        @Schema(description = "Nombre con el que se reconoce el insumo; no puede repetirse", example = "Pelotas de entrenamiento")
        @NotBlank @Size(max = 100) String nombre,
        @Schema(description = "Id de una categoría existente (ver GET /api/categorias-insumo)", example = "2")
        @NotNull @Positive Long categoriaId,
        @Schema(description = "Cantidad inicial en stock, 0 o más", example = "40")
        @NotNull @Min(0) @Max(LimitesDeStock.MAXIMO_STOCK) Integer cantidadDisponible,
        @Schema(description = "Debajo de esta cantidad el stock se considera bajo", example = "10")
        @NotNull @Min(0) @Max(LimitesDeStock.MAXIMO_STOCK) Integer cantidadMinima
) {

    public InsumoRequest {
        nombre = Textos.recortar(nombre);
    }
}
