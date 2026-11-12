package com.tenisahora.modules.insumo.dto;

import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MovimientoResponse(
        @Schema(example = "15") Long id,
        TipoMovimiento tipo,
        @Schema(description = "Negativa si el stock bajó, positiva si subió", example = "-4") Integer variacion,
        @Schema(description = "Cómo quedó el stock después", example = "20") Integer stockResultante,
        @Schema(example = "Rotura en la cancha 2") String motivo,
        LocalDateTime fecha,
        @Schema(description = "Quién lo hizo; vacío si fue el sistema", example = "2") Long personaId
) {
}
