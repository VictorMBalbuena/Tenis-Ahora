package com.tenisahora.modules.instructor.dto;

import com.tenisahora.shared.validation.CodigoArea;
import com.tenisahora.shared.validation.Dni;
import com.tenisahora.shared.validation.NumeroCalle;
import com.tenisahora.shared.validation.NumeroTelefono;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.tenisahora.shared.util.Textos.recortar;

/** Alta de un profesor (CU20) con su título habilitante (RD02) y certificación (CU30). */
@Schema(description = "Alta de un profesor. Repite los datos de persona porque PersonaRequest es un record y no se puede extender.")
public record ProfesorRequest(
        @Schema(example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(example = "Gómez")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @Schema(description = "DNI, solo números. No puede estar repetido.", example = "30123456")
        @NotBlank(message = "El DNI es obligatorio")
        @Dni
        String dni,

        @Schema(example = "1990-05-17")
        @Past(message = "La fecha de nacimiento debe ser pasada")
        LocalDate fechaNacimiento,

        @Schema(example = "Av. Siempreviva")
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 150, message = "La calle no puede superar los 150 caracteres")
        String calle,

        @Schema(description = "Altura, solo números", example = "742")
        @NotBlank(message = "El número de calle es obligatorio")
        @NumeroCalle
        String numeroCalle,

        @Schema(description = "Código de área sin el 0, solo números", example = "11")
        @NotBlank(message = "El código de área es obligatorio")
        @CodigoArea
        String codigoArea,

        @Schema(description = "Número sin el código de área ni el 15, solo números", example = "12345678")
        @NotBlank(message = "El número de teléfono es obligatorio")
        @NumeroTelefono
        String numeroTelefono,

        @Schema(description = "Correo electrónico, único en el sistema", example = "ana@correo.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
        String email,

        @Schema(description = "Contraseña inicial de 8 a 72 caracteres. Nunca se devuelve en una respuesta.",
                example = "Clave-segura-123", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(description = "Título habilitante. Sin esto no se puede registrar un profesor (RD02)", example = "Profesor Nacional de Educación Física")
        @NotBlank(message = "El título habilitante es obligatorio")
        @Size(max = 150, message = "El título habilitante no puede superar los 150 caracteres")
        String tituloHabilitante,

        @Schema(description = "Certificación que respalda el título (CU30)")
        @NotNull(message = "La certificación es obligatoria")
        @Valid
        CertificacionRequest certificacion,

        @Schema(example = "Tenis infantil")
        @Size(max = 30, message = "La especialidad no puede superar los 30 caracteres")
        String especialidad,

        @Schema(example = "8500.00")
        @PositiveOrZero(message = "El valor por hora no puede ser negativo")
        BigDecimal valorPorHora,

        @Schema(description = "Si puede validar certificaciones de otros instructores. Por defecto, no.", example = "false")
        Boolean permisoPersonalAutorizado
) {

    public ProfesorRequest {
        nombre = recortar(nombre);
        apellido = recortar(apellido);
        dni = recortar(dni);
        calle = recortar(calle);
        numeroCalle = recortar(numeroCalle);
        codigoArea = recortar(codigoArea);
        numeroTelefono = recortar(numeroTelefono);
        email = recortar(email);
        tituloHabilitante = recortar(tituloHabilitante);
        especialidad = recortar(especialidad);
    }
}
