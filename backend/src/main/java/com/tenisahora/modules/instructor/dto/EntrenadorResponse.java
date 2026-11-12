package com.tenisahora.modules.instructor.dto;

import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.shared.dto.Identificable;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos completos de un entrenador, incluido el estado de su certificación. Nunca incluye la contraseña ni su hash.")
public record EntrenadorResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "Lucas") String nombre,
        @Schema(example = "Pérez") String apellido,
        @Schema(example = "31123456") String dni,
        @Schema(example = "1992-03-10") LocalDate fechaNacimiento,
        @Schema(description = "Edad calculada a partir de la fecha de nacimiento", example = "33") Integer edad,
        @Schema(example = "Av. Siempreviva") String calle,
        @Schema(example = "742") String numeroCalle,
        @Schema(description = "Código de área sin el 0", example = "11") String codigoArea,
        @Schema(description = "Número sin el código de área ni el 15", example = "12345678") String numeroTelefono,
        @Schema(example = "lucas@correo.com") String email,
        @Schema(example = "ENTRENADOR") Rol rol,
        @Schema(description = "Si la cuenta puede iniciar sesión", example = "true") Boolean activo,
        @Schema(example = "2026-10-10") LocalDate fechaAlta,
        @Schema(example = "Entrenador Nacional de Tenis") String certificacionDeportiva,
        CertificacionResponse certificacion,
        @Schema(example = "PENDIENTE") EstadoValidacion estadoValidacion,
        @Schema(example = "2026-10-15") LocalDate fechaValidacion,
        @Schema(description = "Motivo dejado por el administrador al rechazar la certificación (A1 de CU31)", example = "El archivo del título no es legible")
        String motivoRechazo,
        @Schema(example = "Tenis adultos") String especialidad,
        @Schema(example = "9500.00") BigDecimal valorPorHora
) implements Identificable {

    public static EntrenadorResponse de(PersonaResponse datos, String certificacionDeportiva, CertificacionResponse certificacion,
                                        EstadoValidacion estadoValidacion, LocalDate fechaValidacion, String motivoRechazo,
                                        String especialidad, BigDecimal valorPorHora) {
        return new EntrenadorResponse(datos.id(), datos.nombre(), datos.apellido(), datos.dni(), datos.fechaNacimiento(),
                datos.edad(), datos.calle(), datos.numeroCalle(), datos.codigoArea(), datos.numeroTelefono(),
                datos.email(), datos.rol(), datos.activo(), datos.fechaAlta(), certificacionDeportiva, certificacion,
                estadoValidacion, fechaValidacion, motivoRechazo, especialidad, valorPorHora);
    }
}
