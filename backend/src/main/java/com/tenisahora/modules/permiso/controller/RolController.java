package com.tenisahora.modules.permiso.controller;

import com.tenisahora.modules.permiso.dto.MatrizDePermisos;
import com.tenisahora.modules.permiso.dto.RolRequest;
import com.tenisahora.modules.permiso.dto.RolResponse;
import com.tenisahora.modules.permiso.service.RolService;
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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Roles y permisos: CU85 (crear rol) y CU75 (configurar permisos por rol). Solo el administrador. */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Roles y permisos", description = "Creación de roles y configuración de permisos por rol (CU75, CU85)")
@SecurityRequirement(name = "bearerAuth")
public class RolController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final RolService servicio;

    @GetMapping(produces = JSON)
    @Operation(summary = "Listar roles (CU75)",
            description = "Los cinco roles base y los creados por el administrador, cada uno con su matriz de permisos.")
    public ResponseEntity<List<RolResponse>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @GetMapping(value = "/{id}", produces = JSON)
    @Operation(summary = "Consultar un rol (CU75)")
    @ApiResponse(responseCode = "404", description = "El rol no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RolResponse> consultar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(servicio.consultar(id));
    }

    @PostMapping(consumes = JSON, produces = JSON)
    @Operation(summary = "Crear un rol (CU85)",
            description = "RF85. El nombre no puede estar repetido. El rol se apoya en uno de los cinco roles base "
                    + "y nunca puede dar más permisos que ese rol base.")
    @ApiResponse(responseCode = "409", description = "Ya existe un rol con ese nombre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody RolRequest request, Authentication autenticacion) {
        RolResponse creado = servicio.crear(request, ActorDeSesion.de(autenticacion));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping(value = "/{id}/permisos", consumes = JSON, produces = JSON)
    @Operation(summary = "Configurar los permisos de un rol (CU75)",
            description = "RF75. Se manda la matriz entera: lo que no viene marcado queda sin permiso. Un rol "
                    + "creado no puede recibir permisos que su rol base no tenga.")
    public ResponseEntity<RolResponse> configurarPermisos(@PathVariable @Positive Long id,
                                                          @RequestBody MatrizDePermisos permisos,
                                                          Authentication autenticacion) {
        return ResponseEntity.ok(servicio.configurarPermisos(id, permisos, ActorDeSesion.de(autenticacion)));
    }

    @PatchMapping(value = "/{id}", consumes = JSON, produces = JSON)
    @Operation(summary = "Renombrar un rol creado (CU85)",
            description = "Los cinco roles base no se renombran: solo se les cambian los permisos.")
    public ResponseEntity<RolResponse> renombrar(@PathVariable @Positive Long id,
                                                 @Valid @RequestBody DatosDelRol datos,
                                                 Authentication autenticacion) {
        return ResponseEntity.ok(servicio.renombrar(id, datos.nombre(), datos.descripcion(),
                ActorDeSesion.de(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un rol creado (CU85)",
            description = "Un rol que todavía tiene personas asignadas no se puede eliminar.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "409", description = "El rol está en uso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id, Authentication autenticacion) {
        servicio.eliminar(id, ActorDeSesion.de(autenticacion));
        return ResponseEntity.noContent().build();
    }

    /** Nombre y descripción de un rol creado. */
    public record DatosDelRol(
            @jakarta.validation.constraints.NotBlank(message = "El nombre del rol es obligatorio")
            @Size(min = 2, max = 60, message = "El nombre del rol debe tener entre 2 y 60 caracteres")
            String nombre,
            @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
            String descripcion) {
    }
}
