package com.tenisahora.modules.instructor.dto;

import com.tenisahora.shared.validation.CodigoArea;
import com.tenisahora.shared.validation.Dni;
import com.tenisahora.shared.validation.NumeroCalle;
import com.tenisahora.shared.validation.NumeroTelefono;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.tenisahora.shared.util.Textos.recortar;

/** Modificación parcial de un profesor (CU21). Nulo = no se toca. Cambiar el título o la certificación vuelve el estado a PENDIENTE. */
@Schema(description = "Modificación parcial de un profesor. Solo se cambian los campos enviados. "
        + "Si cambia el título o la certificación, el estado de validación vuelve a PENDIENTE.")
public record ProfesorUpdateRequest(
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
        String email,

        @Schema(description = "Título habilitante (RD02)", example = "Profesor Nacional de Educación Física")
        @Size(min = 1, max = 150, message = "El título habilitante debe tener entre 1 y 150 caracteres")
        String tituloHabilitante,

        @Schema(description = "Si se envía, reemplaza la certificación vigente (CU30)")
        @Valid
        CertificacionRequest certificacion,

        @Schema(example = "Tenis infantil")
        @Size(max = 30, message = "La especialidad no puede superar los 30 caracteres")
        String especialidad,

        @Schema(example = "9000.00")
        @PositiveOrZero(message = "El valor por hora no puede ser negativo")
        BigDecimal valorPorHora,

        @Schema(description = "Si puede validar certificaciones de otros instructores")
        Boolean permisoPersonalAutorizado
) {

    public ProfesorUpdateRequest {
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
