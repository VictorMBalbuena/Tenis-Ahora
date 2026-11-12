package com.tenisahora.modules.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** Grilla de todas las canchas activas para una fecha (CU19). reservable en false: solo se puede mirar, todavía no se puede reservar. */
public record DisponibilidadReservaResponse(
        LocalDate fecha,
        boolean reservable,
        @Schema(description = "Por qué no se puede reservar esa fecha, si corresponde") String aviso,
        List<CanchaDisponibleResponse> canchas
) {
}
