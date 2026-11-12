package com.tenisahora.modules.permiso.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

/** Una cuenta vista desde la administración de roles y permisos (CU74, CU76). */
@Schema(description = "Cuenta con su rol y su estado")
public record CuentaResponse(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String email,

        @Schema(example = "EMPLEADO")
        Rol rol,

        @Schema(description = "Si puede iniciar sesión")
        boolean activa,

        @Schema(description = "Rol personalizado asignado, si tiene uno")
        Long rolPersonalizadoId,

        String rolPersonalizadoNombre) {
}
