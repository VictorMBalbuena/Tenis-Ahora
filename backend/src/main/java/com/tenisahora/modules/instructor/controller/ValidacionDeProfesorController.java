package com.tenisahora.modules.instructor.controller;

import com.tenisahora.shared.seguridad.ActorDeSesion;
import com.tenisahora.modules.instructor.dto.ProfesorResponse;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.service.ProfesorService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesores/{id}")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Validación de profesores", description = "Validación del título habilitante por el administrador (CU31, RD02). Solo administrador.")
@SecurityRequirement(name = "bearerAuth")
public class ValidacionDeProfesorController {

    private final ProfesorService servicio;

    @PostMapping(value = "/validacion", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validar o rechazar el título (CU31)",
            description = "Aprueba (pasa a VALIDADO) o rechaza (pasa a RECHAZADA, con motivo) el título habilitante.")
    @ApiResponse(responseCode = "200", description = "Profesor validado o rechazado")
    @ApiResponse(responseCode = "404", description = "No existe un profesor con ese id")
    @ApiResponse(responseCode = "422", description = "La certificación está vencida, ya fue decidida como válida, o falta el motivo del rechazo")
    public ResponseEntity<ProfesorResponse> validacion(@PathVariable @Positive Long id, @Valid @RequestBody ValidacionRequest request,
                                                       Authentication autenticacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.decidirCertificacion(id, request, ActorDeSesion.de(autenticacion)));
    }
}
