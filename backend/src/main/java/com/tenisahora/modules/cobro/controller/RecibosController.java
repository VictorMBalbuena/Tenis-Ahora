package com.tenisahora.modules.cobro.controller;

import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.modules.cobro.service.ConsultaDeRecibos;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Listado, detalle e impresión de recibos para el personal (CU59, CU61). Un socio ve el recibo desde su cobro
 * ({@code GET /api/cobros/{id}/recibo}).
 */
@RestController
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Recibos", description = "Listado, detalle e impresión de recibos (CU59, CU61). Solo personal.")
@SecurityRequirement(name = "bearerAuth")
public class RecibosController {

    private final CobroService cobroService;
    private final ConsultaDeRecibos consultaDeRecibos;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar recibos (CU59)",
            description = "Por el número de recibo (R-000123, que es el id del cobro que lo originó), con filtro "
                    + "por rango de fecha de emisión. Sin criterio lista todos, los más recientes primero.")
    public ResponseEntity<PageResponse<ReciboResponse>> buscar(
            @Parameter(description = "Número de recibo (R-000123)") @RequestParam(required = false) String busqueda,
            @Parameter(description = "Fecha de emisión, desde") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha de emisión, hasta") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        PageResponse<ReciboResponse> pagina = cobroService.buscarRecibos(busqueda, desde, hasta, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagina);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ver un recibo con el cobro que lo originó (CU59)")
    @ApiResponse(responseCode = "404", description = "No existe ese recibo")
    public ResponseEntity<ReciboResponse> consultar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultaDeRecibos.consultar(id));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Recibo en PDF (CU61)")
    @ApiResponse(responseCode = "404", description = "No existe ese recibo")
    public ResponseEntity<byte[]> pdf(@PathVariable @Positive Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("recibo-" + id + ".pdf").build().toString())
                .body(consultaDeRecibos.imprimir(id));
    }
}
