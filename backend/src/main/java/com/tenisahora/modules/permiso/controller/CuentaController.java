package com.tenisahora.modules.permiso.controller;

import com.tenisahora.modules.permiso.dto.CambioDeEstadoRequest;
import com.tenisahora.modules.permiso.dto.CambioDeRolRequest;
import com.tenisahora.modules.permiso.dto.CuentaResponse;
import com.tenisahora.modules.permiso.dto.MovimientoResponse;
import com.tenisahora.modules.permiso.service.AdministracionDeCuentasService;
import com.tenisahora.shared.dto.ErrorResponse;
import com.tenisahora.shared.seguridad.ActorDeSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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

/**
 * La cuenta de una persona mirada desde la administración, sin importar en qué rol esté: asignarle un rol (CU74)
 * y habilitarla o deshabilitarla (CU76).
 *
 * <p>Es un recurso aparte de /api/socios, /api/empleados y los demás porque justamente trabaja por encima de los
 * cinco: al cambiar de rol, la persona deja de estar en uno de esos recursos y pasa a otro.
 */
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Cuentas", description = "Asignación de roles (CU74) y habilitación o deshabilitación de cuentas (CU76)")
@SecurityRequirement(name = "bearerAuth")
public class CuentaController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final AdministracionDeCuentasService servicio;

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar una cuenta",
            description = "Busca a la persona en los cinco roles y devuelve su rol y su estado.")
    @ApiResponse(responseCode = "404", description = "No existe esa persona",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CuentaResponse> consultar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.consultar(id));
    }

    @GetMapping(value = "/{id}/movimientos", produces = JSON)
    @Operation(summary = "Historial de la cuenta (RF74, RF76)",
            description = "Cambios de rol, bajas y altas, con el motivo, quién lo hizo y cuándo.")
    public ResponseEntity<List<MovimientoResponse>> movimientos(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.historial(id));
    }

    @PutMapping(value = "/{id}/rol", consumes = JSON, produces = JSON)
    @Operation(summary = "Asignar un rol (CU74)",
            description = "RF74. Solo un administrador puede asignar el rol Administrador, y queda registrado quién "
                    + "hizo el cambio y cuándo. Si cambia el rol base, la cuenta se muda a la tabla del rol nuevo y "
                    + "por eso le cambia el id; si ya tiene movimientos con su rol actual, el cambio se rechaza.")
    @ApiResponse(responseCode = "409", description = "La persona tiene historial con su rol actual",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "No se puede asignar ese rol",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CuentaResponse> asignarRol(@PathVariable @Positive Long id,
                                                     @Valid @RequestBody CambioDeRolRequest request,
                                                     Authentication autenticacion) {
        return ResponseEntity.ok(servicio.asignarRol(id, request, ActorDeSesion.de(autenticacion)));
    }

    @PutMapping(value = "/{id}/estado", consumes = JSON, produces = JSON)
    @Operation(summary = "Habilitar o deshabilitar una cuenta (CU76)",
            description = "RF76. Deshabilitar no borra nada: exige un motivo, cierra las sesiones abiertas de la "
                    + "persona y, si se pide, le cancela las reservas de hoy en adelante. Siempre se puede volver "
                    + "a habilitar.")
    @ApiResponse(responseCode = "422", description = "Falta el motivo, o la cuenta ya está en ese estado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CuentaResponse> cambiarEstado(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody CambioDeEstadoRequest request,
                                                        Authentication autenticacion) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, request, ActorDeSesion.de(autenticacion)));
    }
}
