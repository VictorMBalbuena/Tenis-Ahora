package com.tenisahora.modules.instructor.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.EntrenadorRequest;
import com.tenisahora.modules.instructor.dto.EntrenadorResponse;
import com.tenisahora.modules.instructor.dto.EntrenadorUpdateRequest;
import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.service.EntrenadorService;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.dto.PersonaResumenResponse;
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

@RestController
@RequestMapping("/api/entrenadores")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Entrenadores", description = "Alta, consulta, modificación, baja e impresión de entrenadores, con su certificación deportiva (RD03). Solo empleado y administrador, salvo la validación.")
@SecurityRequirement(name = "bearerAuth")
public class EntrenadorController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final EntrenadorService servicio;

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar (CU25)", description = "Crea la cuenta de entrenador con su certificación deportiva y certificación (RD03, CU30). Nace con estado PENDIENTE.")
    @ApiResponse(responseCode = "201", description = "Entrenador creado. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "El DNI, el correo o el número de certificación ya están registrados")
    @ApiResponse(responseCode = "422", description = "Falta la certificación deportiva o la certificación está vencida")
    public ResponseEntity<EntrenadorResponse> registrar(@Valid @RequestBody EntrenadorRequest request, Authentication autenticacion) {
        EntrenadorResponse creado = servicio.registrar(request, ActorDeSesion.de(autenticacion));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creado);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Buscar (CU27)", description = "Por DNI exacto o por nombre (parcial, sin distinguir mayúsculas ni tildes). "
            + "Si vienen los dos, manda el DNI. Sin criterio lista todos, ordenados por apellido y nombre.")
    @ApiResponse(responseCode = "400", description = "El criterio de búsqueda no tiene un formato válido")
    public ResponseEntity<PageResponse<EntrenadorResponse>> buscar(
            @Parameter(description = "DNI exacto, 7 u 8 números", example = "31123456") @RequestParam(required = false) String dni,
            @Parameter(description = "Parte del nombre o apellido (mínimo 2 letras)", example = "perez") @RequestParam(required = false) String nombre,
            @Parameter(description = "Especialidad exacta", example = "Preparación física") @RequestParam(required = false) String especialidad,
            @Parameter(description = "PENDIENTE, VALIDADO o RECHAZADA") @RequestParam(required = false) EstadoValidacion estado,
            @Parameter(description = "Si la cuenta está activa") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.buscar(dni, nombre, especialidad, estado, activo, page, size));
    }

    @GetMapping(value = "/asignables", produces = JSON)
    @Operation(summary = "Listar asignables", description = "Entrenadores con la certificación validada y vigente (RD03), para asignar en BACK 12.")
    public ResponseEntity<PageResponse<PersonaResumenResponse>> asignables(
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamaño de página, de 1 a 50") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.asignables(page, size));
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar (CU27)", description = "Devuelve todos sus datos y los de su certificación, sin la contraseña.")
    @ApiResponse(responseCode = "404", description = "No existe un entrenador con ese id")
    public ResponseEntity<EntrenadorResponse> consultar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.consultar(id));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar (CU26)", description = "Parcial: solo se cambian los campos enviados. "
            + "Si cambia la certificación, el estado de validación vuelve a PENDIENTE.")
    @ApiResponse(responseCode = "409", description = "El DNI, el correo o el número de certificación ya pertenecen a otra persona")
    @ApiResponse(responseCode = "422", description = "La certificación nueva está vencida")
    public ResponseEntity<EntrenadorResponse> modificar(@PathVariable @Positive Long id, @Valid @RequestBody EntrenadorUpdateRequest request,
                                                        Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.modificar(id, request, ActorDeSesion.de(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (CU28)", description = "Borra al entrenador y cierra sus sesiones. La confirmación previa la pide la pantalla.")
    @ApiResponse(responseCode = "204", description = "Entrenador eliminado")
    @ApiResponse(responseCode = "409", description = "Tiene entrenamientos asignados: conviene deshabilitarlo")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id, Authentication autenticacion) {
        servicio.eliminar(id, ActorDeSesion.de(autenticacion));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Imprimir (CU29)", description = "Ficha en PDF con sus datos y el estado de su certificación, sin la contraseña.")
    @ApiResponse(responseCode = "200", description = "PDF con los datos del entrenador", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> imprimir(@PathVariable @Positive Long id) {
        // El nombre del archivo se arma con el id, nunca con texto que escribió alguien
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("entrenador-" + id + ".pdf").build().toString())
                .body(servicio.imprimir(id));
    }

    @PutMapping(value = "/{id}/certificacion", consumes = JSON, produces = JSON)
    @Operation(summary = "Reemplazar la certificación (CU30)", description = "Reemplazo total de la certificación vigente. Vuelve el estado a PENDIENTE.")
    @ApiResponse(responseCode = "404", description = "No existe un entrenador con ese id")
    @ApiResponse(responseCode = "409", description = "El número de certificación ya pertenece a otra")
    @ApiResponse(responseCode = "422", description = "La certificación está vencida")
    public ResponseEntity<EntrenadorResponse> certificacion(@PathVariable @Positive Long id, @Valid @RequestBody CertificacionRequest request,
                                                            Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.reemplazarCertificacion(id, request, ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(value = "/{id}/habilitacion", produces = JSON)
    @Operation(summary = "Consultar la habilitación", description = "Si el entrenador se puede asignar a un entrenamiento (RD03): validado y con la certificación vigente.")
    @ApiResponse(responseCode = "404", description = "No existe un entrenador con ese id")
    public ResponseEntity<HabilitacionResponse> habilitacion(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.habilitacion(id));
    }

    @PostMapping(value = "/{id}/validacion", consumes = JSON, produces = JSON)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Validar o rechazar la certificación (CU31)",
            description = "Solo administrador. Aprueba (pasa a VALIDADO) o rechaza (pasa a RECHAZADA, con motivo) la certificación.")
    @ApiResponse(responseCode = "404", description = "No existe un entrenador con ese id")
    @ApiResponse(responseCode = "422", description = "La certificación está vencida, ya fue decidida como válida, o falta el motivo del rechazo")
    public ResponseEntity<EntrenadorResponse> validacion(@PathVariable @Positive Long id, @Valid @RequestBody ValidacionRequest request,
                                                         Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.decidirCertificacion(id, request, ActorDeSesion.de(autenticacion)));
    }
}
