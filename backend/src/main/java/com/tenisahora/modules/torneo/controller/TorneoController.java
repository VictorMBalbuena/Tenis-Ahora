package com.tenisahora.modules.torneo.controller;

import com.tenisahora.modules.torneo.dto.CrearTorneoRequest;
import com.tenisahora.modules.torneo.dto.ModificarTorneoRequest;
import com.tenisahora.modules.torneo.dto.TorneoResponse;
import com.tenisahora.modules.torneo.service.TorneoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/torneos")
public class TorneoController {

    private final TorneoService torneoService;

    public TorneoController(TorneoService torneoService) {
        this.torneoService = torneoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<TorneoResponse> crearTorneo(@Valid @RequestBody CrearTorneoRequest request) {
        TorneoResponse response = torneoService.crearTorneo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<List<TorneoResponse>> listarTorneos() {
        return ResponseEntity.ok(torneoService.listarTorneos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<TorneoResponse> obtenerTorneo(@PathVariable Long id) {
        return ResponseEntity.ok(torneoService.obtenerTorneo(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<TorneoResponse> modificarTorneo(@PathVariable Long id, @Valid @RequestBody ModificarTorneoRequest request) {
        return ResponseEntity.ok(torneoService.modificarTorneo(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarTorneo(@PathVariable Long id) {
        torneoService.eliminarTorneo(id);
        return ResponseEntity.noContent().build();
    }
}
