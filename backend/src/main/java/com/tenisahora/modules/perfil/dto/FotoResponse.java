package com.tenisahora.modules.perfil.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta de POST /me/foto: la URL ya guardada, así la pantalla no tiene que adivinar dónde quedó. */
@Schema(description = "URL pública de la foto de perfil recién guardada.")
public record FotoResponse(
        @Schema(example = "https://.../fotos/12.webp") String fotoUrl
) {
}
