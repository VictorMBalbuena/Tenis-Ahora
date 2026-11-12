package com.tenisahora.modules.administrador.controller;

import com.tenisahora.modules.administrador.service.AdministradorService;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.persona.ControladorDePersonas;
import com.tenisahora.shared.persona.GestionDePersonas;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administradores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administradores", description = "Alta, consulta, modificación, baja e impresión de administradores. Solo administrador.")
@SecurityRequirement(name = "bearerAuth")
public class AdministradorController extends ControladorDePersonas<PersonaResponse> {

    private final AdministradorService servicio;

    @Override
    protected GestionDePersonas<?, PersonaResponse> gestion() {
        return servicio;
    }

    @Override
    protected String nombreDeArchivo() {
        return "administrador";
    }
}
