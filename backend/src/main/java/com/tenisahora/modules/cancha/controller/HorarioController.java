package com.tenisahora.modules.cancha.controller;

import com.tenisahora.modules.cancha.dto.HorarioRequest;
import com.tenisahora.modules.cancha.dto.HorarioResponse;
import com.tenisahora.modules.cancha.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/canchas/{canchaId}/horarios")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
@Tag(name = "Horarios de canchas", description = "Horarios semanales de disponibilidad (CU090). Solo empleado y administrador.")
@SecurityRequirement(name = "bearerAuth")
public class HorarioController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final HorarioService horarioService;

    @PostMapping(consumes = JSON, produces = JSON)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar disponibilidad (CU090)",
            description = "RF090. Define para cada día indicado el rango de horas en que la cancha está disponible. "
                    + "Todo o nada: si un día se pisa con un horario ya registrado, no se guarda ninguno.")
    @ApiResponse(responseCode = "201", description = "Horarios registrados")
    @ApiResponse(responseCode = "409", description = "Se superpone con un horario ya registrado")
    @ApiResponse(responseCode = "422", description = "La cancha está deshabilitada")
    public List<HorarioResponse> registrar(@PathVariable @Positive Long canchaId, @Valid @RequestBody HorarioRequest request,
                                           @AuthenticationPrincipal Long actorId) {
        return horarioService.registrar(canchaId, request, actorId);
    }

    @GetMapping(produces = JSON)
    @Operation(summary = "Listar horarios de la cancha (CU090)", description = "Ordenados por día de la semana y hora.")
    public List<HorarioResponse> listar(@PathVariable @Positive Long canchaId) {
        return horarioService.listar(canchaId);
    }

    @DeleteMapping("/{horarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar horario (CU090)",
            description = "Quita un horario semanal. Si hay reservas vigentes en esa franja no se borra: primero hay que cancelarlas o reprogramarlas.")
    @ApiResponse(responseCode = "204", description = "Horario eliminado")
    @ApiResponse(responseCode = "409", description = "Hay reservas vigentes en ese horario (el mensaje dice cuáles)")
    public void eliminar(@PathVariable @Positive Long canchaId, @PathVariable @Positive Long horarioId,
                         @AuthenticationPrincipal Long actorId) {
        horarioService.eliminar(canchaId, horarioId, actorId);
    }
}
