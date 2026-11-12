package com.tenisahora.modules.cancha.controller;

import com.tenisahora.modules.cancha.dto.GrillaResponse;
import com.tenisahora.modules.cancha.service.DisponibilidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/canchas/{canchaId}/disponibilidad")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Disponibilidad de canchas", description = "Grilla de horarios libres, ocupados y bloqueados (CU092). Socio, empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar disponibilidad (CU092)",
            description = "RF092. Una franja por hora entre las dos fechas (hasta 31 días): LIBRE, OCUPADA por una reserva o "
                    + "BLOQUEADA por mantenimiento. Sin horarios cargados la lista viene vacía.")
    public GrillaResponse consultar(
            @PathVariable @Positive Long canchaId,
            @Parameter(description = "Primer día", example = "2026-10-20") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Último día, inclusive", example = "2026-10-22") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return disponibilidadService.consultar(canchaId, desde, hasta);
    }
}
