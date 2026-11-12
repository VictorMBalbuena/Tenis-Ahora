package com.tenisahora.modules.socio.dto;

import com.tenisahora.shared.dto.Identificable;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Datos completos de un socio. Nunca incluye la contraseña ni su hash.")
public record SocioResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "Ana") String nombre,
        @Schema(example = "Gómez") String apellido,
        @Schema(example = "30123456") String dni,
        @Schema(example = "1990-05-17") LocalDate fechaNacimiento,
        @Schema(description = "Edad calculada a partir de la fecha de nacimiento", example = "35") Integer edad,
        @Schema(example = "Av. Siempreviva") String calle,
        @Schema(example = "742") String numeroCalle,
        @Schema(description = "Código de área sin el 0", example = "11") String codigoArea,
        @Schema(description = "Número sin el código de área ni el 15", example = "12345678") String numeroTelefono,
        @Schema(example = "ana@correo.com") String email,
        @Schema(example = "SOCIO") Rol rol,
        @Schema(description = "Si la cuenta puede iniciar sesión", example = "true") Boolean activo,
        @Schema(example = "2026-10-10") LocalDate fechaAlta,
        @Schema(example = "S-0012") String numeroSocio
) implements Identificable {

    public static SocioResponse de(PersonaResponse datos, String numeroSocio) {
        return new SocioResponse(datos.id(), datos.nombre(), datos.apellido(), datos.dni(), datos.fechaNacimiento(),
                datos.edad(), datos.calle(), datos.numeroCalle(), datos.codigoArea(), datos.numeroTelefono(),
                datos.email(), datos.rol(), datos.activo(), datos.fechaAlta(), numeroSocio);
    }
}
