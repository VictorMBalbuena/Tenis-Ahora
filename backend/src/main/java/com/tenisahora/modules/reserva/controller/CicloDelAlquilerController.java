package com.tenisahora.modules.reserva.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.service.CicloDelAlquiler;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas/{id}")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Ciclo del alquiler", description = "Inicio y finalización del alquiler de una cancha, con el movimiento de stock (CU14, CU15). Solo personal.")
@SecurityRequirement(name = "bearerAuth")
public class CicloDelAlquilerController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final CicloDelAlquiler cicloDelAlquiler;

    @PostMapping(value = "/inicio", produces = JSON)
    @Operation(summary = "Iniciar el alquiler (CU14)",
            description = "RF14. La reserva confirmada pasa a EN_CURSO y los insumos asignados se descuentan del stock. Se puede desde unos minutos "
                    + "antes del turno hasta que termina. Todo o nada: si un insumo no alcanza, no se descuenta ninguno y la reserva sigue confirmada.")
    @ApiResponse(responseCode = "200", description = "Alquiler iniciado")
    @ApiResponse(responseCode = "422", description = "La reserva no está confirmada, está fuera de la ventana de inicio, o falta stock")
    public ResponseEntity<ReservaResponse> iniciar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cicloDelAlquiler.iniciar(id, ActorDeSesion.de(autenticacion)));
    }

    @PostMapping(value = "/finalizacion", produces = JSON)
    @Operation(summary = "Finalizar el alquiler (CU15)",
            description = "RF15. La reserva en curso pasa a FINALIZADA y los insumos descontados vuelven al stock. Si nadie la registra, "
                    + "se finaliza sola un rato después de terminar el turno.")
    @ApiResponse(responseCode = "200", description = "Alquiler finalizado")
    @ApiResponse(responseCode = "422", description = "La reserva no está en curso")
    public ResponseEntity<ReservaResponse> finalizar(@PathVariable @Positive Long id, Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cicloDelAlquiler.finalizar(id, ActorDeSesion.de(autenticacion)));
    }
}
