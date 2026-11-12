package com.tenisahora.modules.cancha.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Modificación parcial (CU07): solo se cambian los campos que vienen. */
public record CanchaUpdateRequest(
        @Schema(example = "Cancha Central") @Size(min = 1, max = 50) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String identificador,
        @Schema(example = "Cemento / Hormigón") String tipoSuperficie,
        @Schema(example = "2") @Min(2) @Max(4) Integer capacidad,
        @Schema(example = "7500.00") @Positive @Digits(integer = 8, fraction = 2) BigDecimal precioPorHora,
        @Schema(description = "false deshabilita la cancha: deja de aparecer en la grilla", example = "true") Boolean activa,
        @Schema(description = "Id del tipo de cancha asociado (opcional)", example = "1") Long tipoCanchaId,
        @Schema(description = "Si la cancha es cubierta", example = "false") Boolean techada,
        @Schema(description = "Si cuenta con iluminación artificial", example = "true") Boolean iluminacion,
        @Schema(description = "Si cuenta con tribuna", example = "false") Boolean tribuna,
        @Schema(description = "Si está homologada para torneos", example = "true") Boolean aptaTorneos,
        @Schema(description = "Observaciones adicionales", example = "Cancha principal del club")
        @Size(max = 500) String observaciones
) {

    public CanchaUpdateRequest {
        identificador = Textos.recortar(identificador);
    }

    public CanchaUpdateRequest(String identificador, String tipoSuperficie, Integer capacidad, BigDecimal precioPorHora, Boolean activa) {
        this(identificador, tipoSuperficie, capacidad, precioPorHora, activa, null, null, null, null, null, null);
    }

    public CanchaUpdateRequest(String identificador, String tipoSuperficie, Integer capacidad, BigDecimal precioPorHora, Boolean activa, Long tipoCanchaId) {
        this(identificador, tipoSuperficie, capacidad, precioPorHora, activa, tipoCanchaId, null, null, null, null, null);
    }
}
