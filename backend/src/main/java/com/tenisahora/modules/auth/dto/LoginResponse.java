package com.tenisahora.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** El refresh token no viaja acá: va en una cookie httpOnly que el JavaScript del navegador no puede leer. */
@Schema(description = "Sesión iniciada o renovada. El refresh token va aparte, en la cookie httpOnly refresh_token.")
public record LoginResponse(
        @Schema(description = "Token de acceso (JWT). Se manda en el encabezado Authorization: Bearer. Vence a los 30 minutos.")
        String accessToken
) {
}
