package com.tenisahora.modules.cancha.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CanchaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Cancha 1") String identificador,
        @Schema(example = "Polvo de ladrillo (arcilla roja)") String tipoSuperficie,
        @Schema(example = "4") Integer capacidad,
        @Schema(example = "8000.00") BigDecimal precioPorHora,
        @Schema(example = "true") Boolean activa,
        @Schema(description = "true si ahora mismo tiene un bloqueo por mantenimiento", example = "false") boolean enMantenimiento,
        @Schema(example = "1") Long tipoCanchaId,
        @Schema(example = "Polvo de Ladrillo Dobles") String tipoCanchaNombre,
        @Schema(example = "false") Boolean techada,
        @Schema(example = "true") Boolean iluminacion,
        @Schema(example = "false") Boolean tribuna,
        @Schema(example = "true") Boolean aptaTorneos,
        @Schema(example = "Cancha principal") String observaciones,
        @Schema(example = "Polvo de ladrillo (arcilla roja)") String superficie
) {
    public CanchaResponse(Long id, String identificador, String tipoSuperficie, Integer capacidad, BigDecimal precioPorHora, Boolean activa, boolean enMantenimiento) {
        this(id, identificador, tipoSuperficie, capacidad, precioPorHora, activa, enMantenimiento, null, null, false, true, false, false, null, tipoSuperficie);
    }

    public CanchaResponse(Long id, String identificador, String tipoSuperficie, Integer capacidad, BigDecimal precioPorHora, Boolean activa, boolean enMantenimiento, Long tipoCanchaId, String tipoCanchaNombre) {
        this(id, identificador, tipoSuperficie, capacidad, precioPorHora, activa, enMantenimiento, tipoCanchaId, tipoCanchaNombre, false, true, false, false, null, tipoSuperficie);
    }
}
