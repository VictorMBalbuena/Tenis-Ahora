package com.tenisahora.shared.dto;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.model.Sexo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/** Datos propios de la persona autenticada (GET/PATCH /me). Nunca incluye la contraseña ni su hash. */
@Schema(description = "Perfil de la persona autenticada. numeroSocio solo viaja cuando el rol es SOCIO.")
public record MiPerfilResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "Ana") String nombre,
        @Schema(example = "Gómez") String apellido,
        @Schema(example = "30123456") String dni,
        @Schema(example = "1990-05-17") LocalDate fechaNacimiento,
        @Schema(example = "Av. Siempreviva") String calle,
        @Schema(example = "742") String numeroCalle,
        @Schema(description = "Código de área sin el 0", example = "11") String codigoArea,
        @Schema(description = "Número sin el código de área ni el 15", example = "12345678") String numeroTelefono,
        @Schema(example = "ana@correo.com") String email,
        @Schema(description = "Null cuando no tiene foto: la pantalla muestra las iniciales", example = "https://.../fotos/12.webp")
        String fotoUrl,
        @Schema(example = "SOCIO") Rol rol,
        @Schema(description = "Si la cuenta puede iniciar sesión", example = "true") Boolean activo,
        @Schema(example = "2026-03-01") LocalDate fechaAlta,
        @Schema(description = "Solo para socios", example = "S-0012") String numeroSocio,
        @Schema(description = "Define en qué categoría de torneo puede inscribirse") Sexo sexo,
        @Schema(example = "Lanús") String localidad
) implements Identificable {
}
