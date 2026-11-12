package com.tenisahora.modules.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Si un instructor se puede asignar a una clase o a un entrenamiento (RD02, RD03), y por qué no si no se puede. */
@Schema(description = "Si el instructor está en condiciones de ser asignado, con el motivo cuando no lo está.")
public record HabilitacionResponse(
        @Schema(example = "false") boolean asignable,
        @Schema(description = "Motivo por el que no es asignable. Nulo si asignable es true", example = "La certificación está vencida")
        String motivo
) {

    public static HabilitacionResponse deAsignable() {
        return new HabilitacionResponse(true, null);
    }

    public static HabilitacionResponse deNoAsignable(String motivo) {
        return new HabilitacionResponse(false, motivo);
    }
}
