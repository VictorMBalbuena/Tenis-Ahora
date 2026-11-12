package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.MiClaseResponse;
import com.tenisahora.modules.actividad.service.ClasesDelProfesor;
import com.tenisahora.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CU81: lo que el profesor consulta de sus propias clases. Primeros endpoints del rol PROFESOR. */
@RestController
@RequestMapping("/api/clases")
@RequiredArgsConstructor
@Validated
@Tag(name = "Mis clases", description = "Consultar mis clases y mis alumnos (CU81, actor Profesor).")
@SecurityRequirement(name = "bearerAuth")
public class MisClasesController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final ClasesDelProfesor clasesDelProfesor;

    @GetMapping(value = "/mis-clases", produces = JSON)
    @PreAuthorize("hasRole('PROFESOR')")
    @Operation(summary = "Consultar mis clases (CU81)",
            description = "RF81. Las clases del profesor con día, horario, cancha y cupo ocupado. Exige certificación validada.")
    @ApiResponse(responseCode = "422", description = "El profesor no tiene la certificación validada")
    public ResponseEntity<PageResponse<MiClaseResponse>> misClases(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size,
            Authentication autenticacion) {
        return sinCache(clasesDelProfesor.misClases(ActorDeSesion.de(autenticacion), page, size));
    }

    @GetMapping(value = "/{id}/inscriptos", produces = JSON)
    @PreAuthorize("hasAnyRole('PROFESOR', 'EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Consultar los inscriptos de una clase (CU81)",
            description = "El profesor solo ve los de sus propias clases: una ajena responde 404, no 403, así no se filtra su existencia.")
    public ResponseEntity<List<AlumnoDeClaseResponse>> inscriptos(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(clasesDelProfesor.inscriptos(id, ActorDeSesion.de(autenticacion)));
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
