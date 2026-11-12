package com.tenisahora.modules.cancha.controller;

import com.tenisahora.modules.cancha.dto.BloqueoDelDiaResponse;
import com.tenisahora.modules.cancha.dto.BloqueoRequest;
import com.tenisahora.modules.cancha.dto.BloqueoResponse;
import com.tenisahora.modules.cancha.service.BloqueoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/canchas/{canchaId}/bloqueos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Bloqueos de canchas", description = "Bloqueo por mantenimiento y su levantamiento (CU091). Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bloquear por mantenimiento (CU091)",
            description = "RF091, RD05. Bloquea la franja horaria en cada día del rango (hasta 90 días); los horarios bloqueados "
                    + "dejan de figurar como libres. Si hay reservas vigentes (confirmadas o pendientes de pago) en el rango no se bloquea.")
    @ApiResponse(responseCode = "201", description = "Cancha bloqueada")
    @ApiResponse(responseCode = "409", description = "Hay reservas vigentes en el rango (el mensaje dice cuáles) o ya hay un bloqueo")
    @ApiResponse(responseCode = "422", description = "La cancha está deshabilitada")
    public BloqueoResponse bloquear(@PathVariable @Positive Long canchaId, @Valid @RequestBody BloqueoRequest request,
                                    @AuthenticationPrincipal Long actorId) {
        return bloqueoService.bloquear(canchaId, request, actorId);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar bloqueos de la cancha (CU091)",
            description = "Los días bloqueados entre las dos fechas (hasta 90 días), con el id que se usa para levantarlos.")
    public List<BloqueoDelDiaResponse> listar(
            @PathVariable @Positive Long canchaId,
            @Parameter(description = "Primer día", example = "2026-10-20") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Último día, inclusive", example = "2026-10-31") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return bloqueoService.listar(canchaId, desde, hasta);
    }

    @DeleteMapping("/{bloqueoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Levantar bloqueo (CU091)", description = "Desbloquea un día: la franja vuelve a figurar libre en la grilla.")
    @ApiResponse(responseCode = "204", description = "Bloqueo levantado")
    public void desbloquear(@PathVariable @Positive Long canchaId, @PathVariable @Positive Long bloqueoId,
                            @AuthenticationPrincipal Long actorId) {
        bloqueoService.desbloquear(canchaId, bloqueoId, actorId);
    }
}
