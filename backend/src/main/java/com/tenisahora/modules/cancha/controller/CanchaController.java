package com.tenisahora.modules.cancha.controller;

import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.dto.EstadoCancha;
import com.tenisahora.modules.cancha.service.CanchaService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Los errores comunes (400, 401, 403, 404) los agrega OpenApiConfig: acá solo se describe lo propio de cada endpoint. */
@RestController
@RequestMapping("/api/canchas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Canchas", description = "Alta, modificación y consulta de canchas (CU06, CU07, CU08). Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class CanchaController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final CanchaService canchaService;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar cancha (CU06)", description = "RF06. Nace activa. El identificador no puede repetirse.")
    @ApiResponse(responseCode = "201", description = "Cancha creada. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "Ya existe una cancha con ese identificador")
    public ResponseEntity<CanchaResponse> registrar(@Valid @RequestBody CanchaRequest request, @AuthenticationPrincipal Long actorId) {
        CanchaResponse creada = canchaService.registrar(request, actorId);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Buscar canchas (CU08)",
            description = "RF08. Por superficie, estado y/o parte del identificador (sin distinguir mayúsculas ni tildes). "
                    + "Sin filtros lista todas, ordenadas por identificador. Incluye si está en mantenimiento ahora.")
    public PageResponse<CanchaResponse> buscar(
            @Parameter(description = "Superficie de la cancha") @RequestParam(required = false) String superficie,
            @Parameter(description = "Superficie textual de tipo de cancha") @RequestParam(required = false) String superficieTexto,
            @Parameter(description = "Parte del identificador", example = "cancha") @RequestParam(required = false) String identificador,
            @Parameter(description = "DISPONIBLE, MANTENIMIENTO o DESHABILITADA") @RequestParam(required = false) EstadoCancha estado,
            @Parameter(description = "Página, desde 0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        String sup = (superficie != null && !superficie.isBlank()) ? superficie : superficieTexto;
        return canchaService.buscar(sup, identificador, estado, page, size);
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar cancha (CU08)", description = "RF08. Tipo, capacidad, precio y estado de mantenimiento actual.")
    public CanchaResponse consultar(@PathVariable @Positive Long id) {
        return canchaService.consultar(id);
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar cancha (CU07)",
            description = "RF07. Parcial: solo se cambian los campos enviados. Con activa=false se deshabilita.")
    @ApiResponse(responseCode = "200", description = "Cancha modificada")
    @ApiResponse(responseCode = "409", description = "Otra cancha ya tiene ese identificador")
    public CanchaResponse modificar(@PathVariable @Positive Long id, @Valid @RequestBody CanchaUpdateRequest request,
                                    @AuthenticationPrincipal Long actorId) {
        return canchaService.modificar(id, request, actorId);
    }
}
