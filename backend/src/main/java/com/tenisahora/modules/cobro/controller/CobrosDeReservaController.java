package com.tenisahora.modules.cobro.controller;

import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.service.CobroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/reservas/{id}/cobros")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Cobros", description = "Cobro de la seña de una reserva (CU51).")
@SecurityRequirement(name = "bearerAuth")
public class CobrosDeReservaController {

    private final CobroService cobroService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cobrar la seña de una reserva (CU51)",
            description = "RD09 (50% del total), RD21 (descuentos) y RD23 (débito, crédito o QR). El monto lo calcula siempre el servidor. "
                    + "Si el medio queda pagado en el acto (débito o crédito aprobados), emite el recibo y confirma la reserva.")
    @ApiResponse(responseCode = "201", description = "Cobro registrado")
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    @ApiResponse(responseCode = "409", description = "La reserva ya tiene un cobro registrado")
    @ApiResponse(responseCode = "422", description = "La reserva no admite el cobro de la seña, o el medio de pago es inválido")
    public ResponseEntity<CobroResponse> registrar(@PathVariable @Positive Long id, @Valid @RequestBody CobroRequest request,
                                                    Authentication autenticacion) {
        CobroResponse cobro = cobroService.registrar(id, request, ActorDeSesion.de(autenticacion));
        URI ubicacion = URI.create("/api/cobros/" + cobro.id());
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(cobro);
    }
}
