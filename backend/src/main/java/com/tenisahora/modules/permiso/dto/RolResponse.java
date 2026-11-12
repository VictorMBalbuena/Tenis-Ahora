package com.tenisahora.modules.permiso.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/** Un rol con sus permisos, como lo muestra la pantalla de roles (CU75, CU85). */
@Schema(description = "Rol del sistema con su matriz de permisos")
public record RolResponse(
        Long id,
        String nombre,
        String descripcion,

        @Schema(description = "Si es uno de los cinco roles base: se le cambian los permisos pero no se borra")
        boolean base,

        @Schema(description = "Rol base sobre el que se apoya", example = "EMPLEADO")
        Rol rolBase,

        LocalDate fechaAlta,

        @Schema(description = "Cuántas personas lo tienen asignado")
        long cuentas,

        MatrizDePermisos permisos) {
}
