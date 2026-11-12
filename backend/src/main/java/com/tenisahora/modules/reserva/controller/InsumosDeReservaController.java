package com.tenisahora.modules.reserva.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.reserva.dto.AsignacionDeInsumosRequest;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.service.InsumosDeReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Insumos de una reserva", description = "Qué insumos lleva una reserva y cuántos se pueden asignar a un turno (CU13).")
@SecurityRequirement(name = "bearerAuth")
public class InsumosDeReservaController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final InsumosDeReservaService insumosDeReserva;

    @PutMapping(value = "/{id}/insumos", consumes = JSON, produces = JSON)
    @Operation(summary = "Asignar insumos a una reserva (CU13)",
            description = "RF13. Reemplaza los insumos de la reserva: una lista vacía los quita todos. Comprueba que alcancen para ese horario "
                    + "(no cuenta lo que ya se prometió a turnos que se pisan). Solo mientras el alquiler no empezó. "
                    + "El socio titular y el personal pueden; una reserva ajena responde 404.")
    @ApiResponse(responseCode = "200", description = "Reserva con sus insumos asignados")
    @ApiResponse(responseCode = "422", description = "No hay stock suficiente para ese horario, o la reserva ya empezó o está cancelada")
    public ResponseEntity<ReservaResponse> asignar(@PathVariable @Positive Long id, @Valid @RequestBody AsignacionDeInsumosRequest request,
                                                   Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(insumosDeReserva.asignar(id, request, ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(value = "/insumos-disponibles", produces = JSON)
    @Operation(summary = "Insumos disponibles para un turno (CU13, CU11)",
            description = "Los insumos activos con las unidades libres y las que se pueden asignar a ese turno, para ofrecer solo lo que hay.")
    public List<InsumoDisponibleResponse> disponibles(
            @Parameter(description = "Día del turno", example = "2026-10-20") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Hora en punto de inicio", example = "18:00") @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @Parameter(description = "1 o 2 horas", example = "2") @RequestParam @Min(1) @Max(12) int duracionHoras) {
        return insumosDeReserva.disponibles(fecha, horaInicio, duracionHoras);
    }
}
