package com.tenisahora.modules.descuento.controller;

import com.tenisahora.modules.descuento.dto.DescuentoRequest;
import com.tenisahora.modules.descuento.dto.DescuentoResponse;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.service.DescuentoService;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/descuentos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Descuentos", description = "ABM de descuentos (CU62-CU66). Gestión exclusiva de administradores.")
@SecurityRequirement(name = "bearerAuth")
public class DescuentoController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private final DescuentoService descuentoService;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar descuento (CU62)", description = "RF62, RD21, RD22. Solo administradores.")
    @ApiResponse(responseCode = "201", description = "Descuento creado")
    @ApiResponse(responseCode = "400", description = "Beneficio inválido o datos mal formados")
    @ApiResponse(responseCode = "409", description = "Ya existe un descuento activo para ese tipo")
    public ResponseEntity<DescuentoResponse> registrar(@Valid @RequestBody DescuentoRequest request) {
        DescuentoResponse creado = descuentoService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Listar y buscar descuentos (CU64)", description = "RF64. Filtros opcionales por tipo, activo y texto en condiciones.")
    public PageResponse<DescuentoResponse> buscar(
            @RequestParam(required = false) TipoDescuento tipo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return descuentoService.buscar(tipo, activo, busqueda, page, size);
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar descuento (CU64)", description = "RF64. Devuelve el detalle y cantidad de veces aplicado.")
    public DescuentoResponse consultar(@PathVariable @Positive Long id) {
        return descuentoService.consultar(id);
    }

    @PutMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar descuento (CU63)", description = "RF63. Actualiza condiciones, beneficio y estado.")
    public DescuentoResponse modificar(@PathVariable @Positive Long id, @Valid @RequestBody DescuentoRequest request) {
        return descuentoService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar descuento (CU65)", description = "RF65. Baja física si no tiene cobros; bloqueo si fue aplicado.")
    @ApiResponse(responseCode = "204", description = "Descuento eliminado físicamente")
    @ApiResponse(responseCode = "409", description = "El descuento ya fue aplicado a cobros (EN_USO)")
    public void eliminar(@PathVariable @Positive Long id) {
        descuentoService.eliminar(id);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Imprimir descuento (CU66)", description = "RF66. Genera ficha PDF del descuento.")
    @ApiResponse(responseCode = "200", description = "PDF generado", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> imprimir(@PathVariable @Positive Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("descuento-" + id + ".pdf").build().toString())
                .body(descuentoService.imprimir(id));
    }
}
