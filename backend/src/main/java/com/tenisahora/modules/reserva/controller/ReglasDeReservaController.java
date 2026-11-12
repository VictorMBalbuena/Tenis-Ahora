package com.tenisahora.modules.reserva.controller;

import com.tenisahora.modules.reserva.dto.ReglasDeReservaResponse;
import com.tenisahora.modules.reserva.service.ReservasProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas/reglas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Reglas de reserva", description = "Límites vigentes de las reservas, configurados en el servidor.")
@SecurityRequirement(name = "bearerAuth")
public class ReglasDeReservaController {

    private final ReservasProperties propiedades;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar las reglas vigentes de las reservas")
    public ReglasDeReservaResponse consultar() {
        return new ReglasDeReservaResponse(propiedades.antelacionMaximaDias(), propiedades.duracionMaximaHoras(),
                propiedades.porcentajeSenia(), propiedades.plazoCancelacionHoras(),
                propiedades.porcentajeReembolsoATiempo(), propiedades.minutosParaVencerPendientes());
    }
}
