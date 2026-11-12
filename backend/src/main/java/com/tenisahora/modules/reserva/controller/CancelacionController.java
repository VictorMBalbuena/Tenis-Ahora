package com.tenisahora.modules.reserva.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.reserva.dto.CancelacionResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.service.CancelacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas/{id}/cancelacion")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Cancelación de reservas", description = "Vista previa y cancelación de una reserva (CU18).")
@SecurityRequirement(name = "bearerAuth")
public class CancelacionController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final CancelacionService cancelacionService;

    @GetMapping(produces = JSON)
    @Operation(summary = "Ver qué pasaría al cancelar (CU18 A1)",
            description = "No cambia nada. Dice si se cancela a tiempo (6 horas o más) o fuera de plazo, el cargo y el reembolso que corresponden. "
                    + "El personal cancela cualquier reserva; un socio solo las que tiene como titular.")
    @ApiResponse(responseCode = "422", description = "La reserva ya está cancelada o finalizada")
    public ResponseEntity<CancelacionResponse> previsualizar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cancelacionService.previsualizar(id, ActorDeSesion.de(autenticacion)));
    }

    @PostMapping(produces = JSON)
    @Operation(summary = "Cancelar reserva (CU18)",
            description = "RF18, RD12, RD13. A tiempo se reembolsa lo pagado; fuera de plazo se pierde la seña. Libera el horario y los insumos "
                    + "asignados (el stock no se toca). La confirmación previa la pide la pantalla. El reembolso o el cargo reales los hace el cobro (BACK 08).")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada, con el cargo o el reembolso que corresponden")
    @ApiResponse(responseCode = "403", description = "Un jugador que no es el titular no puede cancelar")
    @ApiResponse(responseCode = "422", description = "La reserva ya está cancelada o finalizada")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cancelacionService.cancelar(id, ActorDeSesion.de(autenticacion)));
    }
}
