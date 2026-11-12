package com.tenisahora.modules.reserva.dto;

import com.tenisahora.shared.util.Textos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Un acompañante: una persona registrada (personaId) o un invitado con nombre y apellido (el documento es opcional). */
public record JugadorRequest(
        @Schema(description = "Id de una persona registrada. Si viene, se ignoran los datos de invitado", example = "3") @Positive Long personaId,
        @Schema(description = "Nombre del invitado", example = "Luciana") @Size(max = 100) @Pattern(regexp = SOLO_NOMBRE, message = "El nombre solo admite letras") String nombre,
        @Schema(description = "Apellido del invitado", example = "Fernández") @Size(max = 100) @Pattern(regexp = SOLO_NOMBRE, message = "El apellido solo admite letras") String apellido,
        @Schema(description = "Documento del invitado, opcional", example = "30123456") @Pattern(regexp = "^$|^\\d{7,8}$", message = "El DNI va sin puntos, con 7 u 8 números") String documento
) {

    private static final String SOLO_NOMBRE = "^$|^\\p{L}[\\p{L} .'’-]*$";

    public JugadorRequest {
        nombre = Textos.recortar(nombre);
        apellido = Textos.recortar(apellido);
        documento = Textos.recortar(documento);
    }
}
