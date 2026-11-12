package com.tenisahora.modules.actividad.dto;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record AlumnoDeClaseResponse(
        @Schema(example = "3") Long socioId,
        @Schema(example = "Gómez, Ana") String socio,
        @Schema(example = "30123456") String dni,
        LocalDate fechaInscripcion,
        Long inscripcionId,
        EstadoInscripcion estado
) {
}
