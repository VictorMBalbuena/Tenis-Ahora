package com.tenisahora.modules.insumo.dto;

import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Descontar (CU14) o reponer (CU15) stock. El ajuste manual no va por acá sino por la modificación del insumo. */
public record MovimientoRequest(
        @Schema(description = "DESCUENTO o REPOSICION", example = "DESCUENTO") @NotNull TipoMovimiento tipo,
        @Schema(description = "Unidades, de 1 en adelante", example = "4")
        @NotNull @Min(1) @Max(LimitesDeStock.MAXIMO_POR_MOVIMIENTO) Integer cantidad,
        @Schema(description = "Para qué o por qué; queda en el rastro", example = "Rotura en la cancha 2") @Size(max = 255) String motivo
) {

    public MovimientoRequest {
        motivo = Textos.recortar(motivo);
    }
}
