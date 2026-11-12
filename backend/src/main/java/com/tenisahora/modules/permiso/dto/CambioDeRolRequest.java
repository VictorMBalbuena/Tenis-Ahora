package com.tenisahora.modules.permiso.dto;

import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Asignación de rol a una persona que ya existe (CU74 / RF74). */
@Schema(description = "Rol que pasa a tener la persona (CU74)")
public record CambioDeRolRequest(

        @Schema(description = "Rol base nuevo. Si es el mismo que tiene, solo cambia el rol personalizado.",
                example = "EMPLEADO")
        @NotNull(message = "Hay que indicar el rol")
        Rol rol,

        @Schema(description = "Rol personalizado dentro de ese rol base, o null para usar los permisos del rol base",
                example = "4")
        Long rolPersonalizadoId,

        @Schema(example = "Pasa a atender el mostrador")
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
        String motivo) {

    public CambioDeRolRequest {
        motivo = motivo == null || motivo.isBlank() ? null : motivo.strip();
    }
}
