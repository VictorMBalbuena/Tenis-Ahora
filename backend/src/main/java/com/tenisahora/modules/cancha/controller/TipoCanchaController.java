package com.tenisahora.modules.cancha.controller;

import com.tenisahora.modules.cancha.dto.TipoCanchaRequest;
import com.tenisahora.modules.cancha.dto.TipoCanchaResponse;
import com.tenisahora.modules.cancha.service.TipoCanchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-cancha")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Tipos de Cancha", description = "Gestión de tipos de cancha, superficies y capacidad, y reporte PDF.")
@SecurityRequirement(name = "bearerAuth")
public class TipoCanchaController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final TipoCanchaService tipoCanchaService;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar tipo de cancha", description = "Registra un nuevo tipo con su superficie y capacidad.")
    @ApiResponse(responseCode = "201", description = "Tipo de cancha creado")
    @ApiResponse(responseCode = "409", description = "Ya existe un tipo de cancha con ese nombre")
    public ResponseEntity<TipoCanchaResponse> registrar(@Valid @RequestBody TipoCanchaRequest request,
                                                        @AuthenticationPrincipal Long actorId) {
        TipoCanchaResponse creado = tipoCanchaService.registrar(request, actorId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar tipo de cancha", description = "Actualiza las características (superficie, capacidad, nombre) de un tipo de cancha.")
    public TipoCanchaResponse modificar(@PathVariable @Positive Long id,
                                        @Valid @RequestBody TipoCanchaRequest request,
                                        @AuthenticationPrincipal Long actorId) {
        return tipoCanchaService.modificar(id, request, actorId);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Listar tipos de cancha", description = "Devuelve todos los tipos de cancha activos.")
    public List<TipoCanchaResponse> listar() {
        return tipoCanchaService.listar();
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar tipo de cancha por ID")
    public TipoCanchaResponse consultar(@PathVariable @Positive Long id) {
        return tipoCanchaService.consultar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar tipo de cancha", description = "Baja lógica. Falla con 409 si tiene canchas activas asociadas.")
    public void eliminar(@PathVariable @Positive Long id, @AuthenticationPrincipal Long actorId) {
        tipoCanchaService.eliminar(id, actorId);
    }

    @GetMapping(value = "/reporte", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Generar reporte de tipos de cancha (PDF)", description = "Genera el reporte PDF con los tipos de canchas y sus características.")
    public ResponseEntity<byte[]> descargarReporte() {
        byte[] pdf = tipoCanchaService.generarReportePdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reporte-tipos-cancha.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

