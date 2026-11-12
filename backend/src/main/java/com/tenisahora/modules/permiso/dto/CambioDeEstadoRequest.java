package com.tenisahora.modules.permiso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Habilitación o deshabilitación de una cuenta (CU76 / RF76).
 *
 * <p>El motivo es obligatorio al deshabilitar, como pide RF76; al volver a habilitar es opcional. Esa regla la
 * comprueba el servicio, porque depende del valor de {@code activa}.
 */
@Schema(description = "Alta o baja lógica de una cuenta (CU76)")
public record CambioDeEstadoRequest(

        @Schema(description = "true para habilitar, false para deshabilitar", example = "false")
        @NotNull(message = "Hay que indicar si la cuenta queda habilitada o deshabilitada")
        Boolean activa,

        @Schema(description = "Obligatorio al deshabilitar", example = "Dejó de ser socio del club")
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
        String motivo,

        @Schema(description = "Si además se le cancelan los turnos que tiene reservados de hoy en adelante",
                example = "true")
        Boolean cancelarReservasFuturas) {

    public CambioDeEstadoRequest {
        motivo = motivo == null || motivo.isBlank() ? null : motivo.strip();
        cancelarReservasFuturas = cancelarReservasFuturas != null && cancelarReservasFuturas;
    }
}
