package com.tenisahora.modules.torneo.controller;

import com.tenisahora.modules.torneo.dto.InscribirTorneoRequest;
import com.tenisahora.modules.torneo.dto.InscripcionResponse;
import com.tenisahora.modules.torneo.service.InscripcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/torneos/{id}/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<InscripcionResponse> inscribir(
            @PathVariable Long id,
            @Valid @RequestBody InscribirTorneoRequest request,
            Authentication authentication
    ) {
        InscripcionResponse response = inscripcionService.inscribir(id, request, ActorDeSesion.de(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<List<InscripcionResponse>> listarInscripciones(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionService.listarInscripciones(id));
    }
}
