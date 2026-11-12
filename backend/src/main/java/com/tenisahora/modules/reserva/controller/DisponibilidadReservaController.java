package com.tenisahora.modules.reserva.controller;

import com.tenisahora.modules.reserva.dto.DisponibilidadReservaResponse;
import com.tenisahora.modules.reserva.service.DisponibilidadParaReservar;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/reservas/disponibilidad")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Disponibilidad para reservar", description = "Grilla de todas las canchas para elegir turno (CU19).")
@SecurityRequirement(name = "bearerAuth")
public class DisponibilidadReservaController {

    private final DisponibilidadParaReservar disponibilidad;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar disponibilidad de canchas (CU19)",
            description = "RF19. Una franja por hora de cada cancha activa en la fecha: LIBRE, OCUPADA o BLOQUEADA. Se puede recortar a un rango "
                    + "horario y filtrar por superficie. Más allá de los 30 días de antelación se puede mirar, pero reservable viene en false.")
    public DisponibilidadReservaResponse consultar(
            @Parameter(description = "Día a consultar", example = "2026-10-20") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Desde esta hora; va con horaHasta", example = "16:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaDesde,
            @Parameter(description = "Hasta esta hora; va con horaDesde", example = "21:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaHasta,
            @Parameter(description = "Superficie de la cancha") @RequestParam(required = false) String superficie) {
        return disponibilidad.consultar(fecha, horaDesde, horaHasta, superficie);
    }
}
