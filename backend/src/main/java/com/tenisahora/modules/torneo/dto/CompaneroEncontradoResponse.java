package com.tenisahora.modules.torneo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El socio que se encontró al buscar un compañero de pareja por DNI.
 *
 * Devuelve lo justo para confirmar que es la persona correcta y poder inscribirla: el id no lo escribe nadie,
 * lo usa la pantalla para armar el pedido.
 */
@Schema(description = "Socio encontrado para inscribir como pareja")
public record CompaneroEncontradoResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Luciana") String nombre,
        @Schema(example = "Fernández") String apellido
) {
}
