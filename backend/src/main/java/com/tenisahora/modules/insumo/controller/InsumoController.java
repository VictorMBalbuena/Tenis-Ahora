package com.tenisahora.modules.insumo.controller;

import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.dto.ResumenInsumosResponse;
import com.tenisahora.modules.insumo.service.InsumoService;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Los errores comunes (400, 401, 403, 404) los agrega OpenApiConfig: acá solo se describe lo propio de cada endpoint. */
@RestController
@RequestMapping("/api/insumos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Insumos", description = "Alta, consulta, listado, modificación y baja de insumos (CU10, CU11, CU12, CU101, CU102). Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class InsumoController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final InsumoService insumoService;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar insumo (CU10)", description = "RF10. Nace activo, con su stock inicial. El nombre no puede repetirse.")
    @ApiResponse(responseCode = "201", description = "Insumo creado. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "Ya existe un insumo con ese nombre")
    public ResponseEntity<InsumoResponse> registrar(@Valid @RequestBody InsumoRequest request, @AuthenticationPrincipal Long actorId) {
        InsumoResponse creado = insumoService.registrar(request, actorId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Listar y buscar insumos (CU102, CU11)",
            description = "RF11, RF102. Sin filtros lista todos los activos, ordenados por nombre. Se puede filtrar por parte del nombre "
                    + "(sin distinguir mayúsculas ni tildes), por categoría y por estado de stock.")
    public PageResponse<InsumoResponse> buscar(
            @Parameter(description = "Parte del nombre", example = "pelota") @RequestParam(required = false) @Size(max = 100) String busqueda,
            @Parameter(description = "Id de una categoría (ver GET /api/categorias-insumo)") @RequestParam(required = false) @Positive Long categoriaId,
            @Parameter(description = "DISPONIBLE, STOCK_BAJO o SIN_STOCK") @RequestParam(required = false) EstadoStock estado,
            @Parameter(description = "Página, desde 0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        return insumoService.buscar(busqueda, categoriaId, estado, page, size);
    }

    @GetMapping(value = "/resumen", produces = JSON)
    @Operation(summary = "Resumen de insumos", description = "Totales sobre todos los insumos activos del club (no solo los de una página), para la tira de KPIs del listado.")
    public ResumenInsumosResponse resumen() {
        return insumoService.resumen();
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar insumo (CU11)", description = "RF11. Cantidad disponible, unidades en uso y estado del stock.")
    public InsumoResponse consultar(@PathVariable @Positive Long id) {
        return insumoService.consultar(id);
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar insumo (CU12)",
            description = "RF12. Parcial: solo se cambian los campos enviados. Una cantidad nueva se registra como ajuste en el rastro de movimientos.")
    @ApiResponse(responseCode = "200", description = "Insumo modificado")
    @ApiResponse(responseCode = "409", description = "Otro insumo ya tiene ese nombre")
    public InsumoResponse modificar(@PathVariable @Positive Long id, @Valid @RequestBody InsumoUpdateRequest request,
                                    @AuthenticationPrincipal Long actorId) {
        return insumoService.modificar(id, request, actorId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar insumo (CU101)",
            description = "RF101. Baja lógica: deja de listarse y de ofrecerse en reservas nuevas. Si hay reservas vigentes con el insumo "
                    + "asignado no se da de baja: primero hay que cancelarlas o reprogramarlas. La confirmación previa la pide la pantalla.")
    @ApiResponse(responseCode = "204", description = "Insumo dado de baja")
    @ApiResponse(responseCode = "409", description = "Hay reservas vigentes con este insumo (el mensaje dice cuáles)")
    public void eliminar(@PathVariable @Positive Long id, @AuthenticationPrincipal Long actorId) {
        insumoService.eliminar(id, actorId);
    }
}
