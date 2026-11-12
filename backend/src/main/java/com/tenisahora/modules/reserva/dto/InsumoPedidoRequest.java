package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Un renglón del pedido de insumos de una reserva: cuántas unidades de cuál insumo. */
public record InsumoPedidoRequest(
        @Schema(description = "Id del insumo", example = "1") @NotNull @Positive Long insumoId,
        @Schema(description = "Unidades, de 1 en adelante", example = "4") @NotNull @Min(1) @Max(100) Integer cantidad
) {
}
