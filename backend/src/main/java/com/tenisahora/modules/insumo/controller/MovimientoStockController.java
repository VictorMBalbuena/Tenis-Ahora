package com.tenisahora.modules.insumo.controller;

import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.MovimientoRequest;
import com.tenisahora.modules.insumo.dto.MovimientoResponse;
import com.tenisahora.modules.insumo.service.InsumoStock;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insumos/{insumoId}/movimientos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Movimientos de stock", description = "Descontar y reponer insumos (CU14, CU15) y su rastro. Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class MovimientoStockController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final InsumoStock insumoStock;

    @PostMapping(consumes = JSON, produces = JSON)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Descontar o reponer stock (CU14, CU15)",
            description = "RF14, RF15. Con tipo DESCUENTO resta y con REPOSICION suma. Si se pide descontar más de lo que hay, "
                    + "falla y el stock no cambia: nunca queda negativo. Devuelve el insumo con su stock nuevo.")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado")
    @ApiResponse(responseCode = "422", description = "No hay stock suficiente, o la reposición pasaría el máximo")
    public InsumoResponse mover(@PathVariable @Positive Long insumoId, @Valid @RequestBody MovimientoRequest request,
                                @AuthenticationPrincipal Long actorId) {
        return insumoStock.mover(insumoId, request, actorId);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Rastro de movimientos", description = "Del más reciente al más antiguo: qué pasó, cuánto, cómo quedó el stock, por qué y quién.")
    public PageResponse<MovimientoResponse> historial(
            @PathVariable @Positive Long insumoId,
            @Parameter(description = "Página, desde 0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        return insumoStock.historial(insumoId, page, size);
    }
}
