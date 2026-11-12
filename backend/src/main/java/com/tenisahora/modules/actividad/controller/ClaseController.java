package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.ClaseRequest;
import com.tenisahora.modules.actividad.dto.ClaseResponse;
import com.tenisahora.modules.actividad.dto.ClaseUpdateRequest;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.modules.actividad.service.ClaseService;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Los errores comunes (400, 401, 403, 404) los agrega OpenApiConfig: acá solo se describe lo propio de cada endpoint. */
@RestController
@RequestMapping("/api/clases")
@RequiredArgsConstructor
@Validated

@Tag(name = "Clases", description = "Alta, consulta, modificación y baja de clases (CU32 a CU36).")
@SecurityRequirement(name = "bearerAuth")
public class ClaseController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final ClaseService claseService;

    @PostMapping(consumes = JSON, produces = JSON)
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Programar clase (CU32)",
            description = "RF32. Verifica que el profesor tenga la certificación validada (RD03), que la cancha esté activa, dentro de "
                    + "su horario y sin bloqueo en cada día de dictado, y que no se pise con otra actividad ni con una reserva. El cupo "
                    + "respeta el tope general de configuración (RD01, RD04).")
    @ApiResponse(responseCode = "201", description = "Clase creada. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "El horario ya lo ocupa otra clase, entrenamiento o reserva")
    @ApiResponse(responseCode = "422", description = "Profesor sin certificación validada, cancha fuera de horario o bloqueada, o cupo que supera el límite")
    public ResponseEntity<ClaseResponse> programar(@Valid @RequestBody ClaseRequest request, Authentication autenticacion) {
        ClaseResponse creada = claseService.programar(request, ActorDeSesion.de(autenticacion));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creada);
    }

    @PreAuthorize("hasAnyRole('SOCIO', 'PROFESOR', 'EMPLEADO', 'ADMINISTRADOR')")
    @GetMapping(produces = JSON)
    @Operation(summary = "Buscar clases (CU34)",
            description = "RF34. Filtros combinables por cancha, profesor, fecha, día de la semana, nivel y estado. Sin criterio lista todas.")
    public ResponseEntity<PageResponse<ClaseResponse>> buscar(
            @RequestParam(required = false) @Positive Long canchaId,
            @RequestParam(required = false) @Positive Long profesorId,
            @Parameter(description = "Una fecha dentro del rango de la clase") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) DayOfWeek dia,
            @RequestParam(required = false) NivelClase nivel,
            @RequestParam(required = false) EstadoActividad estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        return sinCache(claseService.buscar(canchaId, profesorId, fecha, dia, nivel, estado, page, size));
    }

    @PreAuthorize("hasAnyRole('SOCIO', 'PROFESOR', 'EMPLEADO', 'ADMINISTRADOR')")
    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar clase (CU34)")
    public ResponseEntity<ClaseResponse> consultar(@PathVariable @Positive Long id) {
        return sinCache(claseService.consultar(id));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Modificar clase (CU33)",
            description = "RF33. Parcial: solo cambia los campos enviados, revalida la agenda contra el nuevo horario y el cupo contra los inscriptos.")
    @ApiResponse(responseCode = "200", description = "Clase modificada")
    @ApiResponse(responseCode = "409", description = "El nuevo horario ya está ocupado")
    @ApiResponse(responseCode = "422", description = "La clase ya no está programada, o el cupo nuevo es menor a los inscriptos activos")
    public ResponseEntity<ClaseResponse> modificar(@PathVariable @Positive Long id, @Valid @RequestBody ClaseUpdateRequest request,
                                                   Authentication autenticacion) {
        return sinCache(claseService.modificar(id, request, ActorDeSesion.de(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Eliminar clase (CU35)",
            description = "RF35. A1: si tiene inscripciones activas, no se elimina hasta cancelarlas.")
    @ApiResponse(responseCode = "204", description = "Clase eliminada")
    @ApiResponse(responseCode = "409", description = "Tiene inscripciones activas")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        claseService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Imprimir ficha de la clase (CU36)", description = "RF36. Ficha en PDF con horario, cancha, profesor y cupo.")
    @ApiResponse(responseCode = "200", description = "PDF con los datos de la clase", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> imprimir(@PathVariable @Positive Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("clase-" + id + ".pdf").build().toString())
                .body(claseService.imprimir(id));
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
