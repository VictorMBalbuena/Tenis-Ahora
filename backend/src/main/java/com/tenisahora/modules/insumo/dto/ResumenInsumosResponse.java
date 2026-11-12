package com.tenisahora.modules.insumo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Totales sobre todos los insumos activos del club, no solo los de una página. */
public record ResumenInsumosResponse(
        @Schema(description = "Cantidad de insumos activos distintos", example = "12") long distintos,
        @Schema(description = "Suma de unidades disponibles en depósito de todos los insumos activos", example = "340") long enDeposito,
        @Schema(description = "Suma de unidades asignadas a reservas que todavía no se repusieron", example = "18") long asignadas,
        @Schema(description = "Cantidad de insumos activos con stock bajo o sin stock", example = "2") long alertas
) {
}
