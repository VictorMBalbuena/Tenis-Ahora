package com.tenisahora.modules.actividad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/** Registro en lote de la asistencia de una fecha (CU42, CU43). Sin fecha, se usa la de hoy. */
public record RegistroDeAsistenciaRequest(
        @Schema(example = "2026-10-19") LocalDate fecha,
        @NotEmpty @Valid List<MarcaDeAsistenciaRequest> marcas
) {
}
