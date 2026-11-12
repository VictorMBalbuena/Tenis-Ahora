package com.tenisahora.modules.actividad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record AsistenciaResponse(
        Long actividadId,
        @Schema(example = "3") Long socioId,
        @Schema(example = "Gómez, Ana") String socio,
        LocalDate fecha,
        Boolean presente
) {
}
