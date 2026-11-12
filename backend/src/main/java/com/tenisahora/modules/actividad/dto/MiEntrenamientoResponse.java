package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.EstadoActividad;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Lo que ve el entrenador en "mis entrenamientos" (CU83): sin datos personales de los alumnos, eso está en /inscriptos. */
public record MiEntrenamientoResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "A-000012") String codigo,
        String nombre,
        String objetivo,
        EstadoActividad estado,
        @Schema(example = "Cancha 1") String cancha,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Set<DayOfWeek> dias,
        @Schema(type = "string", example = "18:00:00") LocalTime horaInicio,
        @Schema(type = "string", example = "19:00:00") LocalTime horaFin,
        Integer cupoMaximo,
        @Schema(description = "Inscriptos activos") Integer cupoOcupado
) {
}
