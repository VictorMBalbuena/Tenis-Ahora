package com.tenisahora.modules.descuento.controller;

import com.tenisahora.modules.descuento.dto.CodigoValidadoResponse;
import com.tenisahora.modules.reserva.service.DescuentoPorCodigo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Validar un código de descuento antes de reservar (RF69).
 *
 * <p>Va separado del ABM de descuentos a propósito: aquel es solo de administradores, y acá entra el socio. Lo
 * único que se expone es si el código sirve y cuánto descuenta; el descuento completo no sale de este módulo.
 *
 * <p>Esto es una vista previa. Lo que se termina cobrando lo vuelve a calcular el servidor al guardar la
 * reserva, con el total real (RD09): el cliente nunca decide un monto.
 */
@RestController
@RequestMapping("/api/descuentos/codigo")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Descuentos", description = "Validación de códigos de descuento al reservar")
@SecurityRequirement(name = "bearerAuth")
public class CodigoDeDescuentoController {

    private final DescuentoPorCodigo descuentoPorCodigo;

    @GetMapping(value = "/{codigo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validar un código de descuento (RF69)",
            description = "Dice si el código existe y está vigente, y cuánto descontaría sobre el monto indicado.")
    @ApiResponse(responseCode = "200", description = "El código sirve")
    @ApiResponse(responseCode = "404", description = "El código no existe o no está vigente")
    public ResponseEntity<CodigoValidadoResponse> validar(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") @PositiveOrZero BigDecimal monto) {

        DescuentoPorCodigo.DescuentoAplicado aplicado = descuentoPorCodigo.aplicar(codigo, monto);

        CodigoValidadoResponse respuesta = new CodigoValidadoResponse(
                aplicado.codigo(), aplicado.descripcion(), monto, aplicado.montoDescontado(), aplicado.montoFinal());

        // Un código se puede dar de baja en cualquier momento: la respuesta no se cachea.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
    }
}
