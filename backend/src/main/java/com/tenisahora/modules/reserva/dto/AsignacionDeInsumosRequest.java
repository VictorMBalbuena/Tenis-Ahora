package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Los insumos que lleva una reserva (CU13). Reemplaza el pedido anterior: una lista vacía los quita todos. */
public record AsignacionDeInsumosRequest(
        @Schema(description = "Hasta 10 insumos distintos; vacía quita todos") @NotNull @Size(max = 10) List<@NotNull @Valid InsumoPedidoRequest> insumos
) {
}
