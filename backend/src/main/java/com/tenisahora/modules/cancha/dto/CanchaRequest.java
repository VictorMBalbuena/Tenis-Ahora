package com.tenisahora.modules.cancha.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Datos para registrar una cancha (CU06). No trae id ni estado: la cancha nace activa. */
public record CanchaRequest(
        @Schema(description = "Nombre con el que se reconoce la cancha", example = "Cancha 3")
        @NotBlank @Size(max = 50) String identificador,
        @Schema(description = "Superficie de la cancha", example = "Polvo de ladrillo (arcilla roja)")
        @NotBlank @Size(max = 100) String tipoSuperficie,
        @Schema(description = "Cantidad de jugadores, de 2 a 4", example = "4")
        @NotNull @Min(2) @Max(4) Integer capacidad,
        @Schema(description = "Precio de la hora", example = "7000.00")
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal precioPorHora,
        @Schema(description = "Id del tipo de cancha asociado (opcional)", example = "1")
        Long tipoCanchaId,
        @Schema(description = "Si la cancha es cubierta", example = "false")
        Boolean techada,
        @Schema(description = "Si cuenta con iluminación artificial", example = "true")
        Boolean iluminacion,
        @Schema(description = "Si cuenta con tribuna", example = "false")
        Boolean tribuna,
        @Schema(description = "Si está homologada para torneos", example = "true")
        Boolean aptaTorneos,
        @Schema(description = "Observaciones adicionales", example = "Cancha principal del club")
        @Size(max = 500) String observaciones
) {

    public CanchaRequest {
        identificador = Textos.recortar(identificador);
        techada = techada != null && techada;
        iluminacion = iluminacion == null || iluminacion;
        tribuna = tribuna != null && tribuna;
        aptaTorneos = aptaTorneos != null && aptaTorneos;
    }

    public CanchaRequest(String identificador, String tipoSuperficie, Integer capacidad, BigDecimal precioPorHora) {
        this(identificador, tipoSuperficie, capacidad, precioPorHora, null, false, true, false, false, null);
    }

    public CanchaRequest(String identificador, String tipoSuperficie, Integer capacidad, BigDecimal precioPorHora, Long tipoCanchaId) {
        this(identificador, tipoSuperficie, capacidad, precioPorHora, tipoCanchaId, false, true, false, false, null);
    }
}
