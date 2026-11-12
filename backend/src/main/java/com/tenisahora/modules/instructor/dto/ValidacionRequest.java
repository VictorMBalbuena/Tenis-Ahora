package com.tenisahora.modules.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.tenisahora.shared.util.Textos.recortar;

/** Decisión del administrador sobre la certificación de un instructor (CU31). El motivo es obligatorio al rechazar. */
@Schema(description = "Decisión sobre la certificación de un profesor o entrenador. Si aprobada es false, el motivo es obligatorio.")
public record ValidacionRequest(
        @Schema(description = "true aprueba, false rechaza", example = "true")
        @NotNull(message = "Hay que indicar si se aprueba o se rechaza")
        Boolean aprobada,

        @Schema(description = "Obligatorio cuando se rechaza", example = "El archivo del título no es legible")
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres")
        String motivo
) {

    public ValidacionRequest {
        motivo = recortar(motivo);
    }
}
