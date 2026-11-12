package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.AsignacionDeInstructorRequest;
import com.tenisahora.modules.actividad.dto.InstructorDisponibleResponse;
import com.tenisahora.modules.actividad.service.AsignacionDeInstructorAActividad;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CU44: asignar un profesor o un entrenador a una clase o entrenamiento particular. */
@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Asignación de instructor", description = "Asignar un profesor o un entrenador a una actividad particular (CU44).")
@SecurityRequirement(name = "bearerAuth")
public class AsignacionDeInstructorController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final AsignacionDeInstructorAActividad asignacion;

    @GetMapping(value = "/{id}/instructores-disponibles", produces = JSON)
    @Operation(summary = "Instructores disponibles (CU44)",
            description = "RF44. Profesores y entrenadores con la certificación validada y sin otra actividad que se pise con este horario.")
    @ApiResponse(responseCode = "422", description = "La actividad no es particular")
    public ResponseEntity<List<InstructorDisponibleResponse>> disponibles(@PathVariable @Positive Long id) {
        return sinCache(asignacion.disponibles(id));
    }

    @PutMapping(value = "/{id}/instructor", consumes = JSON)
    @Operation(summary = "Asignar instructor (CU44)",
            description = "RF44. Reemplaza el instructor de la actividad particular. Vuelve a validar la certificación del elegido.")
    @ApiResponse(responseCode = "200", description = "Instructor asignado")
    @ApiResponse(responseCode = "422", description = "Sin certificación validada, o la actividad no es particular")
    public ResponseEntity<Void> asignar(@PathVariable @Positive Long id, @Valid @RequestBody AsignacionDeInstructorRequest request,
                                        Authentication autenticacion) {
        asignacion.asignar(id, request, ActorDeSesion.de(autenticacion));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
