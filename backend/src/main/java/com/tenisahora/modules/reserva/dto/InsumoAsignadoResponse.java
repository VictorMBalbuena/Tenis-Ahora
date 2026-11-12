package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InsumoAsignadoResponse(
        @Schema(example = "1") Long insumoId,
        @Schema(example = "Raquetas") String nombre,
        @Schema(example = "4") int cantidad,
        EstadoDeAsignacion estado
) {
}
