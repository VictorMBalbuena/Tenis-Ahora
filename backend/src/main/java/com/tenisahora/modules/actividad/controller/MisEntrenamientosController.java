package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.MiEntrenamientoResponse;
import com.tenisahora.modules.actividad.service.EntrenamientosDelEntrenador;
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

/** CU83: lo que el entrenador consulta de sus propios entrenamientos. Espejo de MisClasesController. */
@RestController
@RequestMapping("/api/entrenamientos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Mis entrenamientos", description = "Consultar mis entrenamientos y mis alumnos (CU83, actor Entrenador).")
@SecurityRequirement(name = "bearerAuth")
public class MisEntrenamientosController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final EntrenamientosDelEntrenador entrenamientosDelEntrenador;

    @GetMapping(value = "/mis-entrenamientos", produces = JSON)
    @PreAuthorize("hasRole('ENTRENADOR')")
    @Operation(summary = "Consultar mis entrenamientos (CU83)",
            description = "RF83. Los entrenamientos del entrenador con día, horario, cancha y cupo ocupado. Exige certificación validada.")
    @ApiResponse(responseCode = "422", description = "El entrenador no tiene la certificación validada")
    public ResponseEntity<PageResponse<MiEntrenamientoResponse>> misEntrenamientos(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo es 1") @Max(value = 50, message = "El tamaño máximo es 50") int size,
            Authentication autenticacion) {
        return sinCache(entrenamientosDelEntrenador.misEntrenamientos(ActorDeSesion.de(autenticacion), page, size));
    }

    @GetMapping(value = "/{id}/inscriptos", produces = JSON)
    @PreAuthorize("hasAnyRole('ENTRENADOR', 'EMPLEADO', 'ADMINISTRADOR')")
    @Operation(summary = "Consultar los inscriptos de un entrenamiento (CU83)",
            description = "El entrenador solo ve los de sus propios entrenamientos: uno ajeno responde 404, no 403, así no se filtra su existencia.")
    public ResponseEntity<List<AlumnoDeClaseResponse>> inscriptos(@PathVariable @Positive Long id, Authentication autenticacion) {
        return sinCache(entrenamientosDelEntrenador.inscriptos(id, ActorDeSesion.de(autenticacion)));
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
