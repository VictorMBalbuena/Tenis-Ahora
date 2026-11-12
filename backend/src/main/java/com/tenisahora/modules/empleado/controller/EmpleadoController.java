package com.tenisahora.modules.empleado.controller;

import com.tenisahora.modules.empleado.service.EmpleadoService;
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
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Empleados", description = "Alta, consulta, modificación, baja e impresión de empleados. Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class EmpleadoController extends ControladorDePersonas<PersonaResponse> {

    private final EmpleadoService servicio;

    @Override
    protected GestionDePersonas<?, PersonaResponse> gestion() {
        return servicio;
    }

    @Override
    protected String nombreDeArchivo() {
        return "empleado";
    }
}
