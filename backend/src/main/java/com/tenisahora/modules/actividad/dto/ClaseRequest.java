package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Datos para programar una clase (CU32). No lleva estado ni código: los pone el servidor. */
public record ClaseRequest(
        @Schema(example = "Tenis para principiantes") @NotNull @Size(min = 1, max = 80) String nombre,
        @Schema(example = "GRUPAL") @NotNull Modalidad modalidad,
        @Schema(example = "INICIAL") @NotNull NivelClase nivel,
        @Schema(example = "1") @NotNull @Positive Long canchaId,
        @Schema(example = "4") @NotNull @Positive Long profesorId,
        @Schema(example = "2026-11-02") @NotNull LocalDate fechaInicio,
        @Schema(example = "2027-03-02") @NotNull LocalDate fechaFin,
        @Schema(description = "Días de la semana en que se dicta") @NotEmpty Set<DayOfWeek> dias,
        @Schema(description = "Hora en punto", example = "18:00", type = "string") @NotNull LocalTime horaInicio,
        @Schema(description = "Hora en punto", example = "19:00", type = "string") @NotNull LocalTime horaFin,
        @Schema(description = "Si no se indica, se usa el valor por defecto de configuración", example = "20") @Min(1) Integer cupoMaximo,
        @Schema(example = "4") @PositiveOrZero Integer cupoMinimo,
        @Schema(example = "15000.00") @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal valorMensual,
        @Schema(example = "Adultos") @Size(max = 30) String categoria
) {

    public ClaseRequest {
        nombre = Textos.recortar(nombre);
        categoria = Textos.recortar(categoria);
    }
}
