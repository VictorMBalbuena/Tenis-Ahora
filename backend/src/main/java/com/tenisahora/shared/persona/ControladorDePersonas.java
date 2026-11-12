package com.tenisahora.shared.persona;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.Identificable;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.dto.PersonaRequest;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.seguridad.ActorDeSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@Validated
public abstract class ControladorDePersonas<R extends Identificable> {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    protected abstract GestionDePersonas<?, R> gestion();
    protected abstract String nombreDeArchivo();

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar", description = "Crea la cuenta con el rol de este recurso. El DNI y el correo no pueden estar usados por ninguna otra persona del club.")
    @ApiResponse(responseCode = "201", description = "Persona creada. El encabezado Location apunta a su consulta")
    @ApiResponse(responseCode = "409", description = "El DNI o el correo ya están registrados")
    @ApiResponse(responseCode = "422", description = "La contraseña supera los 72 bytes")
    public ResponseEntity<R> registrar(@Valid @RequestBody PersonaRequest request, Authentication authentication) {
        R creada = gestion().registrar(request, actor(authentication));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creada);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Buscar",
            description = "Por DNI exacto o por nombre (parcial, sin distinguir mayúsculas ni tildes). Si vienen los dos, manda el DNI. "
                    + "Sin criterio lista todas, ordenadas por apellido y nombre.")
    public ResponseEntity<PageResponse<PersonaResumenResponse>> buscar(
            @Parameter(description = "DNI exacto, 7 u 8 números", example = "30123456")
            @RequestParam(required = false) String dni,
            @Parameter(description = "Parte del nombre o apellido (mínimo 2 letras)", example = "gomez")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Página, desde 0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Tamaño de página, de 1 a 50")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(gestion().buscar(dni, nombre, page, size));
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar", description = "Devuelve todos sus datos, sin la contraseña.")
    public ResponseEntity<R> consultar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(gestion().consultar(id));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Modificar", description = "Parcial: solo se cambian los campos enviados.")
    @ApiResponse(responseCode = "200", description = "Persona modificada")
    @ApiResponse(responseCode = "409", description = "El DNI o el correo ya pertenecen a otra persona")
    public ResponseEntity<R> modificar(@PathVariable @Positive Long id, @Valid @RequestBody PersonaUpdateRequest request,
                                       Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(gestion().modificar(id, request, actor(authentication)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar", description = "Borra a la persona y cierra sus sesiones. La confirmación previa la pide la pantalla.")
    @ApiResponse(responseCode = "204", description = "Persona eliminada")
    @ApiResponse(responseCode = "409", description = "Tiene reservas, inscripciones o asistencias: conviene deshabilitarla")
    @ApiResponse(responseCode = "422", description = "Es tu propia cuenta o el único administrador")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id, Authentication authentication) {
        gestion().eliminar(id, actor(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Imprimir", description = "Ficha en PDF, sin la contraseña.")
    @ApiResponse(responseCode = "200", description = "PDF con los datos de la persona",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    public ResponseEntity<byte[]> imprimir(@PathVariable @Positive Long id) {
        // El nombre del archivo se arma con el id, nunca con texto que escribió alguien
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreDeArchivo() + "-" + id + ".pdf").build().toString())
                .body(gestion().imprimir(id));
    }

    private Actor actor(Authentication authentication) {
        return ActorDeSesion.de(authentication);
    }
}
