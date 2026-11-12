package com.tenisahora.modules.torneo.controller;

import com.tenisahora.modules.torneo.dto.InscripcionResponse;
import com.tenisahora.modules.torneo.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/torneos/inscripciones")
@RequiredArgsConstructor
public class MisInscripcionesATorneosController {

    private final InscripcionService inscripcionService;

    @GetMapping("/mias")
    @PreAuthorize("hasRole('SOCIO')")
    public List<InscripcionResponse> mias(Authentication authentication) {
        return inscripcionService.delSocio(ActorDeSesion.de(authentication).id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public InscripcionResponse detalle(@PathVariable Long id, Authentication authentication) {
        return inscripcionService.detalle(id, ActorDeSesion.de(authentication));
    }
}
