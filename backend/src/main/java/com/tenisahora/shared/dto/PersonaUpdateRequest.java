package com.tenisahora.shared.dto;

import com.tenisahora.shared.validation.CodigoArea;
import com.tenisahora.shared.validation.Dni;
import com.tenisahora.shared.validation.NumeroCalle;
import com.tenisahora.shared.validation.NumeroTelefono;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static com.tenisahora.shared.util.Textos.recortar;

/** Modificación parcial (CU02): los campos que no se envían (o llegan nulos) no se tocan. */
@Schema(description = "Modificación parcial de una persona (CU02 / RF02). Solo se cambian los campos enviados. "
        + "El rol, el estado y la contraseña no se modifican por acá.")
public record PersonaUpdateRequest(
        @Schema(example = "Ana")
        @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
        String nombre,

        @Schema(example = "Gómez")
        @Size(min = 1, max = 100, message = "El apellido debe tener entre 1 y 100 caracteres")
        String apellido,

        @Schema(description = "DNI, solo números. No puede estar repetido.", example = "30123456")
        @Dni
        String dni,

        @Schema(example = "1990-05-17")
        @Past(message = "La fecha de nacimiento debe ser pasada")
        LocalDate fechaNacimiento,

        @Schema(example = "Av. Siempreviva")
        @Size(min = 1, max = 150, message = "La calle debe tener entre 1 y 150 caracteres")
        String calle,

        @Schema(description = "Altura, solo números", example = "742")
        @NumeroCalle
        String numeroCalle,

        @Schema(description = "Código de área sin el 0, solo números", example = "11")
        @CodigoArea
        String codigoArea,

        @Schema(description = "Número sin el código de área ni el 15, solo números", example = "12345678")
        @NumeroTelefono
        String numeroTelefono,

        @Schema(description = "Correo electrónico, único en el sistema", example = "ana@correo.com")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
        String email
) {

    /** Se recortan los espacios antes de validar: un espacio de más al escribir no tiene que invalidar el dato. La contraseña no se toca. */
    public PersonaUpdateRequest {
        nombre = recortar(nombre);
        apellido = recortar(apellido);
        dni = recortar(dni);
        calle = recortar(calle);
        numeroCalle = recortar(numeroCalle);
        codigoArea = recortar(codigoArea);
        numeroTelefono = recortar(numeroTelefono);
        email = recortar(email);
    }
}
