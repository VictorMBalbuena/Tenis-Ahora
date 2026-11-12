package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.EntrenamientoRequest;
import com.tenisahora.modules.actividad.dto.EntrenamientoResponse;
import com.tenisahora.modules.actividad.dto.EntrenamientoUpdateRequest;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.service.EntrenamientoService;
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

/** Espejo de ClaseController. Los errores comunes (400, 401, 403, 404) los agrega OpenApiConfig. */
@RestController
@RequestMapping("/api/entrenamientos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Entrenamientos", description = "Alta, consulta, modificación y baja de entrenamientos (CU37 a CU41).")
@SecurityRequirement(name = "bearerAuth")
public class EntrenamientoController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final EntrenamientoService entrenamientoService;

    @PostMapping(consumes = JSON, produces = JSON)
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Programar entrenamiento (CU37)",
            description = "RF37. Verifica que el entrenador tenga la certificación validada (RD03), que la cancha esté activa, dentro de "
                    + "su horario y sin bloqueo en cada día de dictado, y que no se pise con otra actividad ni con una reserva. El cupo "
                    + "respeta el tope general de configuración (RD01, RD04).")
    @ApiResponse(responseCode = "201", description = "Entrenamiento creado. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "El horario ya lo ocupa otra clase, entrenamiento o reserva")
    @ApiResponse(responseCode = "422", description = "Entrenador sin certificación validada, cancha fuera de horario o bloqueada, o cupo que supera el límite")
    public ResponseEntity<EntrenamientoResponse> programar(@Valid @RequestBody EntrenamientoRequest request, Authentication autenticacion) {
        EntrenamientoResponse creado = entrenamientoService.programar(request, ActorDeSesion.de(autenticacion));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creado);
    }

    @PreAuthorize("hasAnyRole('SOCIO', 'ENTRENADOR', 'EMPLEADO', 'ADMINISTRADOR')")
    @GetMapping(produces = JSON)
    @Operation(summary = "Buscar entrenamientos (CU39)",
            description = "RF39. Filtros combinables por cancha, entrenador, fecha, día de la semana y estado. Sin criterio lista todos.")
    public ResponseEntity<PageResponse<EntrenamientoResponse>> buscar(
            @RequestParam(required = false) @Positive Long canchaId,
            @RequestParam(required = false) @Positive Long entrenadorId,
            @Parameter(description = "Una fecha dentro del rango del entrenamiento") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) DayOfWeek dia,
            @RequestParam(required = false) EstadoActividad estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        return sinCache(entrenamientoService.buscar(canchaId, entrenadorId, fecha, dia, estado, page, size));
    }

    @PreAuthorize("hasAnyRole('SOCIO', 'ENTRENADOR', 'EMPLEADO', 'ADMINISTRADOR')")
    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar entrenamiento (CU39)")
    public ResponseEntity<EntrenamientoResponse> consultar(@PathVariable @Positive Long id) {
        return sinCache(entrenamientoService.consultar(id));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Modificar entrenamiento (CU38)",
            description = "RF38. Parcial: solo cambia los campos enviados, revalida la agenda contra el nuevo horario y el cupo contra los inscriptos.")
    @ApiResponse(responseCode = "200", description = "Entrenamiento modificado")
    @ApiResponse(responseCode = "409", description = "El nuevo horario ya está ocupado")
    @ApiResponse(responseCode = "422", description = "El entrenamiento ya no está programado, o el cupo nuevo es menor a los inscriptos activos")
    public ResponseEntity<EntrenamientoResponse> modificar(@PathVariable @Positive Long id, @Valid @RequestBody EntrenamientoUpdateRequest request,
                                                           Authentication autenticacion) {
        return sinCache(entrenamientoService.modificar(id, request, ActorDeSesion.de(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Eliminar entrenamiento (CU40)",
            description = "RF40. A1: si tiene inscripciones activas, no se elimina hasta cancelarlas.")
    @ApiResponse(responseCode = "204", description = "Entrenamiento eliminado")
    @ApiResponse(responseCode = "409", description = "Tiene inscripciones activas")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        entrenamientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Imprimir ficha del entrenamiento (CU41)", description = "RF41. Ficha en PDF con horario, cancha, entrenador, objetivo y cupo.")
    @ApiResponse(responseCode = "200", description = "PDF con los datos del entrenamiento", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> imprimir(@PathVariable @Positive Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("entrenamiento-" + id + ".pdf").build().toString())
                .body(entrenamientoService.imprimir(id));
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
