package com.tenisahora.modules.actividad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Modalidad;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record EntrenamientoResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "A-000012") String codigo,
        String nombre,
        Modalidad modalidad,
        String objetivo,
        EstadoActividad estado,
        Long canchaId,
        @Schema(example = "Cancha 1") String cancha,
        Long entrenadorId,
        @Schema(example = "Pérez, Juan") String entrenador,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Set<DayOfWeek> dias,
        @Schema(type = "string", example = "18:00:00") LocalTime horaInicio,
        @Schema(type = "string", example = "19:00:00") LocalTime horaFin,
        Integer cupoMaximo,
        Integer cupoMinimo,
        @Schema(description = "Inscriptos activos") Integer cupoOcupado,
        @Schema(description = "Lugares que quedan libres") Integer cupoDisponible,
        BigDecimal valorMensual,
        String categoria
) {

    @JsonProperty("admiteInscripcion")
    public boolean admiteInscripcion() {
        return estado != null && estado.admiteInscripcion();
    }
}
