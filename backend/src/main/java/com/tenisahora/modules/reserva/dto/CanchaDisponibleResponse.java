package com.tenisahora.modules.reserva.dto;

import com.tenisahora.modules.cancha.dto.FranjaResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record CanchaDisponibleResponse(
        Long canchaId,
        @Schema(example = "Cancha 1") String identificador,
        @Schema(example = "Polvo de ladrillo (arcilla roja)") String tipoSuperficie,
        @Schema(example = "8000.00") BigDecimal precioPorHora,
        List<FranjaResponse> franjas
) {
}
