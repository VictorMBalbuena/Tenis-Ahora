package com.tenisahora.modules.torneo.controller;

import com.tenisahora.modules.torneo.dto.CargarResultadoRequest;
import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.dto.PosicionResponse;
import com.tenisahora.modules.torneo.service.FixtureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/torneos/{id}")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @PostMapping("/fixture")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<List<PartidoResponse>> generarFixture(@PathVariable Long id) {
        return ResponseEntity.ok(fixtureService.generarFixture(id));
    }

    @GetMapping("/partidos")
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<List<PartidoResponse>> listarPartidos(@PathVariable Long id) {
        return ResponseEntity.ok(fixtureService.listarPartidos(id));
    }

    /** La tabla de posiciones de una liga (CU49). Un torneo se sigue por su cuadro, no por una tabla. */
    @GetMapping("/posiciones")
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<List<PosicionResponse>> tablaDePosiciones(@PathVariable Long id) {
        return ResponseEntity.ok(fixtureService.tablaDePosiciones(id));
    }

    @GetMapping(value = "/resultados/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarPdfResultados(@PathVariable Long id) {
        byte[] pdf = fixtureService.generarPdfResultados(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resultados-torneo-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/partidos/{partidoId}/resultado")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    public ResponseEntity<PartidoResponse> cargarResultado(
            @PathVariable Long id,
            @PathVariable Long partidoId,
            @Valid @RequestBody CargarResultadoRequest request
    ) {
        return ResponseEntity.ok(fixtureService.cargarResultado(id, partidoId, request));
    }
}
