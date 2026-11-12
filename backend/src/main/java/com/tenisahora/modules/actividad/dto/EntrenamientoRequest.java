package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.Modalidad;
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

/** Datos para programar un entrenamiento (CU37). No lleva estado ni código: los pone el servidor. */
public record EntrenamientoRequest(
        @Schema(example = "Entrenamiento funcional") @NotNull @Size(min = 1, max = 80) String nombre,
        @Schema(example = "GRUPAL") @NotNull Modalidad modalidad,
        @Schema(example = "Mejorar el saque y el revés") @Size(max = 200) String objetivo,
        @Schema(example = "1") @NotNull @Positive Long canchaId,
        @Schema(example = "5") @NotNull @Positive Long entrenadorId,
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

    public EntrenamientoRequest {
        nombre = Textos.recortar(nombre);
        objetivo = Textos.recortar(objetivo);
        categoria = Textos.recortar(categoria);
    }
}
