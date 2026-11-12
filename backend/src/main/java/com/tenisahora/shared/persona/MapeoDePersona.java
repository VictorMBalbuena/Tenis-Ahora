package com.tenisahora.shared.persona;

import com.tenisahora.shared.dto.MiPerfilResponse;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.util.Textos;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** Convierte los datos comunes entre entidades y DTOs. Nunca copia la contraseña ni su hash hacia afuera. */
@Component
public class MapeoDePersona {

    public PersonaResponse aRespuesta(Persona persona) {
        return new PersonaResponse(
                persona.getId(),
                persona.getNombre(),
                persona.getApellido(),
                persona.getDni(),
                persona.getFechaNacimiento(),
                persona.calcularEdad(),
                persona.getCalle(),
                persona.getNumeroCalle(),
                persona.getCodigoArea(),
                persona.getNumeroTelefono(),
                persona.getEmail(),
                persona.getRol(),
                persona.getActivo(),
                persona.getFechaAlta());
    }

    /** Para /me: igual que aRespuesta, pero con la foto y el número de socio (solo lo que cada rol conoce de sí mismo). */
    public MiPerfilResponse aMiPerfil(Persona persona, String numeroSocio) {
        return new MiPerfilResponse(
                persona.getId(),
                persona.getNombre(),
                persona.getApellido(),
                persona.getDni(),
                persona.getFechaNacimiento(),
                persona.getCalle(),
                persona.getNumeroCalle(),
                persona.getCodigoArea(),
                persona.getNumeroTelefono(),
                persona.getEmail(),
                persona.getFotoUrl(),
                persona.getRol(),
                persona.getActivo(),
                persona.getFechaAlta(),
                numeroSocio,
                persona.getSexo(),
                persona.getLocalidad());
    }

    public PersonaResumenResponse aResumen(Persona persona) {
        return new PersonaResumenResponse(
                persona.getId(),
                persona.getNombre(),
                persona.getApellido(),
                persona.getDni(),
                persona.getEmail(),
                persona.getRol(),
                persona.getActivo());
    }

    /** Modificación parcial: solo se tocan los campos que vinieron en el pedido. */
    public void aplicar(PersonaUpdateRequest cambios, Persona persona) {
        siHay(cambios.nombre(), persona::setNombre);
        siHay(cambios.apellido(), persona::setApellido);
        siHay(cambios.dni(), persona::setDni);
        siHay(cambios.fechaNacimiento(), persona::setFechaNacimiento);
        siHay(cambios.calle(), persona::setCalle);
        siHay(cambios.numeroCalle(), persona::setNumeroCalle);
        siHay(cambios.codigoArea(), persona::setCodigoArea);
        siHay(cambios.numeroTelefono(), persona::setNumeroTelefono);
        siHay(cambios.email(), email -> persona.setEmail(Textos.normalizarEmail(email)));
    }

    private <T> void siHay(T valor, Consumer<T> asignar) {
        if (valor != null) {
            asignar.accept(valor);
        }
    }
}
