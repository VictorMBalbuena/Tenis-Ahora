package com.tenisahora.modules.permiso.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de un rol nuevo (CU85 / RF85). */
@Schema(description = "Rol nuevo definido por el administrador (CU85)")
public record RolRequest(

        @Schema(example = "Recepción")
        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(min = 2, max = 60, message = "El nombre del rol debe tener entre 2 y 60 caracteres")
        String nombre,

        @Schema(example = "Atiende el mostrador pero no anula cobros")
        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
        String descripcion,

        @Schema(description = "Rol base sobre el que se apoya: define en qué tabla vive la persona y qué puede "
                + "llegar a hacer como máximo", example = "EMPLEADO")
        @NotNull(message = "Hay que indicar sobre qué rol base se apoya")
        Rol rolBase,

        @Schema(description = "Permisos iniciales. Si no se manda nada, el rol arranca sin permisos.")
        MatrizDePermisos permisos) {

    public RolRequest {
        nombre = nombre == null ? null : nombre.strip();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
    }
}
