package com.tenisahora.modules.socio.controller;

import com.tenisahora.modules.socio.dto.SocioResponse;
import com.tenisahora.modules.socio.service.SocioService;
import com.tenisahora.shared.persona.ControladorDePersonas;
import com.tenisahora.shared.persona.GestionDePersonas;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/socios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Socios", description = "Alta, consulta, modificación, baja e impresión de socios (CU01 a CU05). Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class SocioController extends ControladorDePersonas<SocioResponse> {

    private final SocioService servicio;

    @Override
    protected GestionDePersonas<?, SocioResponse> gestion() {
        return servicio;
    }

    @Override
    protected String nombreDeArchivo() {
        return "socio";
    }
}
