package com.tenisahora.modules.instructor.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.dto.ProfesorRequest;
import com.tenisahora.modules.instructor.dto.ProfesorResponse;
import com.tenisahora.modules.instructor.dto.ProfesorUpdateRequest;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.service.ProfesorService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * CU20-CU24. No extiende ControladorDePersonas: el alta necesita el título habilitante y la certificación (RD02),
 * que PersonaRequest no tiene. Delega en los métodos públicos de ProfesorService, duplicando anotaciones, no lógica.
 */
@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Profesores", description = "Alta, consulta, modificación, baja e impresión de profesores, con su título habilitante (RD02). Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class ProfesorController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final ProfesorService servicio;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar (CU20)", description = "Crea la cuenta de profesor con su título habilitante y certificación (RD02, CU30). Nace con estado PENDIENTE.")
    @ApiResponse(responseCode = "201", description = "Profesor creado. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "El DNI, el correo o el número de certificación ya están registrados")
    @ApiResponse(responseCode = "422", description = "Falta el título habilitante o la certificación está vencida")
    public ResponseEntity<ProfesorResponse> registrar(@Valid @RequestBody ProfesorRequest request, Authentication autenticacion) {
        ProfesorResponse creado = servicio.registrar(request, ActorDeSesion.de(autenticacion));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creado);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Buscar (CU22)", description = "Por DNI exacto, nombre (parcial, sin distinguir mayúsculas ni tildes), "
            + "estado de validación y/o si está activo. Si vienen DNI y nombre, manda el DNI. Sin criterios lista todos, "
            + "ordenados por apellido y nombre.")
    public ResponseEntity<PageResponse<ProfesorResponse>> buscar(
            @Parameter(description = "DNI exacto, 7 u 8 números", example = "30123456") @RequestParam(required = false) String dni,
            @Parameter(description = "Parte del nombre o apellido (mínimo 2 letras)", example = "gomez") @RequestParam(required = false) String nombre,
            @Parameter(description = "PENDIENTE o VALIDADO") @RequestParam(required = false) EstadoValidacion estado,
            @Parameter(description = "Si la cuenta puede iniciar sesión") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.buscar(dni, nombre, estado, activo, page, size));
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar (CU22)", description = "Devuelve todos sus datos y los de su título, sin la contraseña.")
    @ApiResponse(responseCode = "404", description = "No existe un profesor con ese id")
    public ResponseEntity<ProfesorResponse> consultar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(id));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar (CU21)", description = "Parcial: solo se cambian los campos enviados. "
            + "Si cambia el título o la certificación, el estado de validación vuelve a PENDIENTE.")
    @ApiResponse(responseCode = "409", description = "El DNI, el correo o el número de certificación ya pertenecen a otra persona")
    @ApiResponse(responseCode = "422", description = "La certificación nueva está vencida")
    public ResponseEntity<ProfesorResponse> modificar(@PathVariable @Positive Long id, @Valid @RequestBody ProfesorUpdateRequest request,
                                                      Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.modificar(id, request, ActorDeSesion.de(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (CU23)", description = "Borra al profesor y cierra sus sesiones. La confirmación previa la pide la pantalla.")
    @ApiResponse(responseCode = "204", description = "Profesor eliminado")
    @ApiResponse(responseCode = "409", description = "Tiene clases asignadas: conviene deshabilitarlo")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id, Authentication autenticacion) {
        servicio.eliminar(id, ActorDeSesion.de(autenticacion));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Imprimir (CU24)", description = "Ficha en PDF con sus datos y el estado de su título, sin la contraseña.")
    @ApiResponse(responseCode = "200", description = "PDF con los datos del profesor", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> imprimir(@PathVariable @Positive Long id) {
        // El nombre del archivo se arma con el id, nunca con texto que escribió alguien
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("profesor-" + id + ".pdf").build().toString())
                .body(servicio.imprimir(id));
    }

    @PutMapping(value = "/{id}/certificacion", consumes = JSON, produces = JSON)
    @Operation(summary = "Reemplazar el título (CU30)", description = "Reemplazo total de la certificación vigente. Vuelve el estado a PENDIENTE.")
    @ApiResponse(responseCode = "404", description = "No existe un profesor con ese id")
    @ApiResponse(responseCode = "409", description = "El número de certificación ya pertenece a otra")
    @ApiResponse(responseCode = "422", description = "La certificación está vencida")
    public ResponseEntity<ProfesorResponse> certificacion(@PathVariable @Positive Long id, @Valid @RequestBody CertificacionRequest request,
                                                          Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.reemplazarCertificacion(id, request, ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(value = "/{id}/habilitacion", produces = JSON)
    @Operation(summary = "Consultar la habilitación", description = "Si el profesor se puede asignar a una clase (RD02): validado y con el título vigente.")
    @ApiResponse(responseCode = "404", description = "No existe un profesor con ese id")
    public ResponseEntity<HabilitacionResponse> habilitacion(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.habilitacion(id));
    }
}
