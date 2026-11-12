package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Modificación parcial de una clase (CU33): solo se cambian los campos que vienen. */
public record ClaseUpdateRequest(
        @Schema(example = "Tenis para principiantes") @Size(min = 1, max = 80) String nombre,
        @Schema(example = "GRUPAL") Modalidad modalidad,
        @Schema(example = "INICIAL") NivelClase nivel,
        @Schema(example = "2") @Positive Long canchaId,
        @Schema(example = "4") @Positive Long profesorId,
        @Schema(example = "2026-11-02") LocalDate fechaInicio,
        @Schema(example = "2027-03-02") LocalDate fechaFin,
        @Schema(description = "Días de la semana en que se dicta") @Size(min = 1) Set<DayOfWeek> dias,
        @Schema(example = "19:00", type = "string") LocalTime horaInicio,
        @Schema(example = "20:00", type = "string") LocalTime horaFin,
        @Schema(example = "20") @Min(1) Integer cupoMaximo,
        @Schema(example = "4") @PositiveOrZero Integer cupoMinimo,
        @Schema(example = "15000.00") @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal valorMensual,
        @Schema(example = "Adultos") @Size(max = 30) String categoria
) {

    public ClaseUpdateRequest {
        nombre = Textos.recortar(nombre);
        categoria = Textos.recortar(categoria);
    }
}
