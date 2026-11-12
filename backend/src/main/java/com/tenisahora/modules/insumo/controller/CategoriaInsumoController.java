package com.tenisahora.modules.insumo.controller;

import com.tenisahora.modules.insumo.dto.CategoriaInsumoRequest;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.service.CategoriaInsumoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Categorías de insumo (Raqueta, Pelota, Red, ...): el club puede sumar categorías nuevas sin tocar código. */
@RestController
@RequestMapping("/api/categorias-insumo")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Insumos", description = "Categorías de insumo. Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaInsumoController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final CategoriaInsumoService categoriaInsumoService;

    @GetMapping(produces = JSON)
    @Operation(summary = "Listar categorías de insumo", description = "Todas las categorías, ordenadas por nombre.")
    public List<CategoriaInsumoResponse> listar() {
        return categoriaInsumoService.listar();
    }

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar una categoría de insumo nueva")
    @ApiResponse(responseCode = "201", description = "Categoría creada")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    public ResponseEntity<CategoriaInsumoResponse> registrar(@Valid @RequestBody CategoriaInsumoRequest request) {
        CategoriaInsumoResponse creada = categoriaInsumoService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar una categoría de insumo", description = "Le cambia el nombre. No afecta a los insumos que ya la tienen asignada.")
    @ApiResponse(responseCode = "200", description = "Categoría modificada")
    @ApiResponse(responseCode = "409", description = "Otra categoría ya tiene ese nombre")
    public CategoriaInsumoResponse modificar(@PathVariable @Positive Long id, @Valid @RequestBody CategoriaInsumoRequest request) {
        return categoriaInsumoService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una categoría de insumo",
            description = "No se puede si hay insumos (activos o dados de baja) con esta categoría asignada.")
    @ApiResponse(responseCode = "204", description = "Categoría eliminada")
    @ApiResponse(responseCode = "409", description = "Hay insumos con esta categoría")
    public void eliminar(@PathVariable @Positive Long id) {
        categoriaInsumoService.eliminar(id);
    }
}
