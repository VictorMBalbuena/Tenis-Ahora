package com.tenisahora.modules.actividad.controller;

import com.tenisahora.modules.actividad.dto.AsistenciaResponse;
import com.tenisahora.modules.actividad.dto.RegistroDeAsistenciaRequest;
import com.tenisahora.modules.actividad.service.RegistroDeAsistencias;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Registrar y consultar la asistencia de una clase o de un entrenamiento (CU42, CU43): mismo endpoint, distinto actor. */
@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('PROFESOR', 'ENTRENADOR', 'EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Asistencia", description = "Registrar y consultar la asistencia de una clase o un entrenamiento (CU42, CU43).")
@SecurityRequirement(name = "bearerAuth")
public class AsistenciaController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final RegistroDeAsistencias registroDeAsistencias;

    @PostMapping(value = "/{id}/asistencias", consumes = JSON, produces = JSON)
    @Operation(summary = "Registrar asistencia (CU42, CU43)",
            description = "RF42, RF43. El profesor o el entrenador marcan presente o ausente a cada alumno de su propia actividad en lote. "
                    + "La fecha tiene que caer dentro de la vigencia de la actividad y en uno de sus días de dictado; volver a registrar "
                    + "la misma fecha corrige en vez de duplicar.")
    @ApiResponse(responseCode = "200", description = "Asistencia registrada")
    @ApiResponse(responseCode = "404", description = "Actividad ajena o inexistente")
    @ApiResponse(responseCode = "422", description = "Fecha sin dictado, o un socio marcado que no está inscripto")
    public ResponseEntity<List<AsistenciaResponse>> registrar(@PathVariable @Positive Long id,
                                                               @Valid @RequestBody RegistroDeAsistenciaRequest request,
                                                               Authentication autenticacion) {
        return sinCache(registroDeAsistencias.registrar(id, request.fecha(), request.marcas(), ActorDeSesion.de(autenticacion)));
    }

    @GetMapping(value = "/{id}/asistencias", produces = JSON)
    @Operation(summary = "Consultar la asistencia de una fecha", description = "Los inscriptos con su marca; ausente si todavía no se registró.")
    public ResponseEntity<List<AsistenciaResponse>> delDia(@PathVariable @Positive Long id,
                                                            @Parameter(description = "Por defecto, hoy") @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                            Authentication autenticacion) {
        return sinCache(registroDeAsistencias.delDia(id, fecha, ActorDeSesion.de(autenticacion)));
    }

    private <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}
