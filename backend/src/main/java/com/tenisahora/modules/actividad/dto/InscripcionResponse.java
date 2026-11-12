package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record InscripcionResponse(
        @Schema(example = "45") Long id,
        Long actividadId,
        Long socioId,
        @Schema(example = "Gómez, Ana") String socio,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado
) {
}
