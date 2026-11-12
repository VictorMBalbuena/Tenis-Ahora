package com.tenisahora.modules.perfil.dto;

import com.tenisahora.shared.model.Sexo;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.validation.CodigoArea;
import com.tenisahora.shared.validation.NumeroCalle;
import com.tenisahora.shared.validation.NumeroTelefono;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static com.tenisahora.shared.util.Textos.recortar;

/**
 * PATCH /me: modificación parcial de los datos propios. A diferencia de {@link PersonaUpdateRequest} no tiene DNI
 * (identifica a la persona en el club y en el carnet) ni rol: no existen en esta clase, así que no hay nada que
 * filtrar ni que rechazar si llegan.
 */
@Schema(description = "Modificación parcial del perfil propio. Solo se cambian los campos enviados. "
        + "El DNI y el rol no se pueden tocar desde acá: no forman parte de este pedido.")
public record MiPerfilUpdateRequest(
        @Schema(example = "Ana")
        @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
        String nombre,

        @Schema(example = "Gómez")
        @Size(min = 1, max = 100, message = "El apellido debe tener entre 1 y 100 caracteres")
        String apellido,

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
        String email,
        @Schema(description = "Define en qué categoría de torneo puede inscribirse", example = "FEMENINO")
        Sexo sexo,
        @Schema(example = "Lanús")
        @Size(min = 1, max = 100, message = "La localidad debe tener entre 1 y 100 caracteres")
        String localidad
) {

    public MiPerfilUpdateRequest {
        nombre = recortar(nombre);
        apellido = recortar(apellido);
        calle = recortar(calle);
        numeroCalle = recortar(numeroCalle);
        codigoArea = recortar(codigoArea);
        numeroTelefono = recortar(numeroTelefono);
        email = recortar(email);
        localidad = recortar(localidad);
    }

    /** El puente hacia GestionDePersonas: mismo formato que usa el CRUD de personas, con el DNI siempre en null. */
    public PersonaUpdateRequest aPersonaUpdateRequest() {
        return new PersonaUpdateRequest(nombre, apellido, null, fechaNacimiento, calle, numeroCalle, codigoArea,
                numeroTelefono, email);
    }
}
