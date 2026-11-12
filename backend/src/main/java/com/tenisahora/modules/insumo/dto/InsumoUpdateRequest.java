package com.tenisahora.modules.insumo.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Modificación parcial (CU12): solo se cambian los campos que vienen. La cantidad se corrige como un ajuste y queda en el rastro. */
public record InsumoUpdateRequest(
        @Schema(example = "Pelotas de torneo") @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        @Schema(description = "Id de una categoría existente", example = "2") @Positive Long categoriaId,
        @Schema(example = "12") @Min(0) @Max(LimitesDeStock.MAXIMO_STOCK) Integer cantidadMinima,
        @Schema(description = "Cantidad real tras una rotura o pérdida: se registra como ajuste. No puede ser negativa", example = "38")
        @Min(0) @Max(LimitesDeStock.MAXIMO_STOCK) Integer cantidadDisponible
) {

    public InsumoUpdateRequest {
        nombre = Textos.recortar(nombre);
    }
}
