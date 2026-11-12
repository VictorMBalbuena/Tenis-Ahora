package com.tenisahora.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tenisahora.shared.exception.CodigoError;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único de error de toda la API (DIS 06). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error de la API. Es el mismo formato para todos los endpoints.")
public record ErrorResponse(
        @Schema(description = "Código estable del error, para que el front decida qué mostrar", example = "PERSONA_DNI_DUPLICADO")
        String codigo,
        @Schema(description = "Mensaje en español para la persona", example = "Ya existe una persona con ese DNI")
        String mensaje,
        @Schema(description = "Detalle por campo cuando falla la validación (campo → mensaje)")
        Map<String, String> campos,
        @Schema(description = "Momento del error")
        LocalDateTime fecha,
        @Schema(description = "Ruta que se pidió", example = "/api/personas")
        String ruta
) {

    public static ErrorResponse de(CodigoError codigo, String mensaje, String ruta) {
        return new ErrorResponse(codigo.name(), mensaje, null, LocalDateTime.now(), ruta);
    }

    public static ErrorResponse conCampos(CodigoError codigo, String mensaje, Map<String, String> campos, String ruta) {
        return new ErrorResponse(codigo.name(), mensaje, campos, LocalDateTime.now(), ruta);
    }
}
