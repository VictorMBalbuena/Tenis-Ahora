package com.tenisahora.modules.cobro.controller;

import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cobros")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Cobros", description = "Listado de cobros (CU54). Solo personal.")
@SecurityRequirement(name = "bearerAuth")
public class CobrosController {

    private final CobroService cobroService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar cobros (CU54)",
            description = "Por el código del cobro (C-000123) o el número de reserva (R-000123), con filtros por "
                    + "concepto, medio de pago y rango de fecha. Sin criterio lista todos, los más recientes primero.")
    public ResponseEntity<PageResponse<CobroResponse>> buscar(
            @Parameter(description = "Código de cobro (C-000123) o número de reserva (R-000123)") @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) ConceptoCobro concepto,
            @RequestParam(required = false) MedioPago medioPago,
            @Parameter(description = "Fecha del cobro, desde") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha del cobro, hasta") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        PageResponse<CobroResponse> pagina = cobroService.buscar(busqueda, concepto, medioPago, desde, hasta, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagina);
    }
}
