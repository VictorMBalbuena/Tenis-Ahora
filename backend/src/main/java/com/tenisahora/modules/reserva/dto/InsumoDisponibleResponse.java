package com.tenisahora.modules.reserva.dto;

import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record InsumoDisponibleResponse(
        Long insumoId,
        @Schema(example = "Raquetas") String nombre,
        CategoriaInsumoResponse categoria,
        @Schema(description = "Unidades libres hoy en el depósito", example = "24") int cantidadLibre,
        @Schema(description = "Unidades que se pueden asignar a ese turno: las libres menos las ya prometidas a turnos que se pisan", example = "20") int cantidadAsignable
) {
}
