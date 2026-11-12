package com.tenisahora.modules.instructor.dto;

import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.shared.dto.Identificable;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos completos de un profesor, incluido el estado de su título. Nunca incluye la contraseña ni su hash.")
public record ProfesorResponse(
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
        @Schema(example = "PROFESOR") Rol rol,
        @Schema(description = "Si la cuenta puede iniciar sesión", example = "true") Boolean activo,
        @Schema(example = "2026-10-10") LocalDate fechaAlta,
        @Schema(example = "Profesor Nacional de Educación Física") String tituloHabilitante,
        CertificacionResponse certificacion,
        @Schema(example = "VALIDADO") EstadoValidacion estadoValidacion,
        @Schema(example = "2026-10-15") LocalDate fechaValidacion,
        @Schema(example = "Tenis infantil") String especialidad,
        @Schema(example = "8500.00") BigDecimal valorPorHora,
        @Schema(description = "Si puede validar certificaciones de otros instructores", example = "false") Boolean permisoPersonalAutorizado
) implements Identificable {

    public static ProfesorResponse de(PersonaResponse datos, String tituloHabilitante, CertificacionResponse certificacion,
        EstadoValidacion estadoValidacion, LocalDate fechaValidacion,
        String especialidad, BigDecimal valorPorHora, Boolean permisoPersonalAutorizado) {
        return new ProfesorResponse(datos.id(), datos.nombre(), datos.apellido(), datos.dni(), datos.fechaNacimiento(),
                datos.edad(), datos.calle(), datos.numeroCalle(), datos.codigoArea(), datos.numeroTelefono(),
                datos.email(), datos.rol(), datos.activo(), datos.fechaAlta(), tituloHabilitante, certificacion,
                estadoValidacion, fechaValidacion, especialidad, valorPorHora, permisoPersonalAutorizado);
    }
}
