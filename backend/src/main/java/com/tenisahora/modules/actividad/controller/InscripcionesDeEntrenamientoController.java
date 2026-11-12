package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.InscripcionRequest;
import com.tenisahora.modules.actividad.dto.InscripcionResponse;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.service.InscripcionesDeActividad;
import com.tenisahora.shared.exception.CodigoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Inscripción de un socio a un entrenamiento (CU82). Espejo de InscripcionesDeClaseController. */
@RestController
@RequestMapping("/api/entrenamientos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Inscripciones a entrenamientos", description = "Inscribir y cancelar la inscripción de un socio a un entrenamiento (CU82).")
@SecurityRequirement(name = "bearerAuth")
public class InscripcionesDeEntrenamientoController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final InscripcionesDeActividad inscripcionesDeActividad;

    @PostMapping(value = "/{id}/inscripciones", consumes = JSON, produces = JSON)
    @Operation(summary = "Inscribir a un entrenamiento (CU82)",
            description = "RF82. Un socio se inscribe a sí mismo; el personal indica a quién inscribir. Si había cancelado antes, "
                    + "la reinscripción reactiva la fila en vez de duplicarla.")
    @ApiResponse(responseCode = "201", description = "Inscripción registrada")
    @ApiResponse(responseCode = "403", description = "Un socio intentó inscribir a otra persona")
    @ApiResponse(responseCode = "409", description = "Ya estaba inscripto")
    @ApiResponse(responseCode = "422", description = "El entrenamiento ya no admite inscripciones o no tiene cupo disponible")
    public ResponseEntity<InscripcionResponse> inscribir(@PathVariable @Positive Long id, @Valid @RequestBody(required = false) InscripcionRequest request,
                                                         Authentication autenticacion) {
        InscripcionResponse creada = inscripcionesDeActividad.inscribir(id, Entrenamiento.class, CodigoError.ENTRENAMIENTO_NO_ENCONTRADO,
                requestONulo(request), ActorDeSesion.de(autenticacion));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).cacheControl(CacheControl.noStore()).body(creada);
    }

    @DeleteMapping("/{id}/inscripciones/{socioId}")
    @Operation(summary = "Cancelar una inscripción", description = "El socio cancela la propia; el personal, cualquiera.")
    @ApiResponse(responseCode = "204", description = "Inscripción cancelada")
    @ApiResponse(responseCode = "403", description = "Un socio intentó cancelar una inscripción ajena")
    public ResponseEntity<Void> cancelar(@PathVariable @Positive Long id, @PathVariable @Positive Long socioId, Authentication autenticacion) {
        inscripcionesDeActividad.cancelar(id, CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, socioId, ActorDeSesion.de(autenticacion));
        return ResponseEntity.noContent().build();
    }

    private InscripcionRequest requestONulo(InscripcionRequest request) {
        return request != null ? request : new InscripcionRequest(null);
    }
}
