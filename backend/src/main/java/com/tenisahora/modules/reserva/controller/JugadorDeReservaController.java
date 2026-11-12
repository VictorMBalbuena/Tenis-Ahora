package com.tenisahora.modules.reserva.controller;

import com.tenisahora.modules.reserva.dto.JugadorEncontradoResponse;
import com.tenisahora.modules.reserva.service.BusquedaDeJugador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas/jugadores")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Jugadores de una reserva", description = "Búsqueda de un socio para sumarlo como acompañante.")
@SecurityRequirement(name = "bearerAuth")
public class JugadorDeReservaController {

    private final BusquedaDeJugador busquedaDeJugador;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar un socio activo por DNI exacto")
    public JugadorEncontradoResponse buscar(@RequestParam @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 números") String dni) {
        return busquedaDeJugador.porDni(dni);
    }
}
