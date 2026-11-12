package com.tenisahora.modules.cobro.controller;

import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.service.CobroDeInscripcionActividad;
import com.tenisahora.modules.cobro.service.CobroDeInscripcionTorneo;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SOCIO', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Cobros", description = "Cobro de inscripciones a clases, entrenamientos y torneos.")
@SecurityRequirement(name = "bearerAuth")
public class CobrosDeInscripcionController {

    private final CobroDeInscripcionActividad cobroDeActividad;
    private final CobroDeInscripcionTorneo cobroDeTorneo;

    @PostMapping(value = "/api/actividades/inscripciones/{id}/cobros", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cobrar la seña o el saldo de una inscripción a una clase o un entrenamiento",
            description = "El primer cobro es el 50% del valor y confirma el lugar; el siguiente, el saldo. El monto lo calcula el servidor.")
    public ResponseEntity<CobroResponse> cobrarActividad(@PathVariable @Positive Long id, @Valid @RequestBody CobroRequest request,
                                                          Authentication autenticacion) {
        return creado(cobroDeActividad.registrar(id, request, ActorDeSesion.de(autenticacion)));
    }

    @PostMapping(value = "/api/torneos/inscripciones/{id}/cobros", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cobrar el arancel de una inscripción a un torneo (CU52)")
    public ResponseEntity<CobroResponse> cobrarTorneo(@PathVariable @Positive Long id, @Valid @RequestBody CobroRequest request,
                                                       Authentication autenticacion) {
        return creado(cobroDeTorneo.registrar(id, request, ActorDeSesion.de(autenticacion)));
    }

    private ResponseEntity<CobroResponse> creado(CobroResponse cobro) {
        return ResponseEntity.created(URI.create("/api/cobros/" + cobro.id())).cacheControl(CacheControl.noStore()).body(cobro);
    }
}
