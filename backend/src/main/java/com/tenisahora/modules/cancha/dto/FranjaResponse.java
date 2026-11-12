package com.tenisahora.modules.cancha.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

public record FranjaResponse(
        LocalDate fecha,
        @Schema(example = "08:00:00", type = "string") LocalTime desde,
        @Schema(example = "09:00:00", type = "string") LocalTime hasta,
        EstadoFranja estado,
        @Schema(example = "RESERVA", description = "Origen de ocupación: RESERVA, CLASE, MANTENIMIENTO o null si LIBRE")
        String tipo
) {
    public FranjaResponse(LocalDate fecha, LocalTime desde, LocalTime hasta, EstadoFranja estado) {
        this(fecha, desde, hasta, estado, null);
    }
}
