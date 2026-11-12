package com.tenisahora.shared.persona;

import com.tenisahora.shared.dto.MiPerfilResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.model.Sexo;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Todo lo de /me (consulta, modificación, contraseña y foto): la propia persona sobre sus propios datos, sin Actor
 * ni permiso de rol. Separado de GestionDePersonas solo para no pasar el límite de líneas por clase; obtener,
 * guardar y asegurarQueNoChoca son los mismos métodos privados de GestionDePersonas, pasados como referencia.
 */
@Slf4j
final class MiPerfilDePersona<E extends Persona> {

    private final Function<Long, E> obtener;
    private final UnaryOperator<E> guardar;
    private final BiConsumer<E, PersonaUpdateRequest> asegurarQueNoChoca;
    private final Function<E, String> numeroSocioDe;
    private final MapeoDePersona mapeo;
    private final PasswordEncoder passwordEncoder;

    MiPerfilDePersona(Function<Long, E> obtener, UnaryOperator<E> guardar,
                      BiConsumer<E, PersonaUpdateRequest> asegurarQueNoChoca, Function<E, String> numeroSocioDe,
                      MapeoDePersona mapeo, PasswordEncoder passwordEncoder) {
        this.obtener = obtener;
        this.guardar = guardar;
        this.asegurarQueNoChoca = asegurarQueNoChoca;
        this.numeroSocioDe = numeroSocioDe;
        this.mapeo = mapeo;
        this.passwordEncoder = passwordEncoder;
    }

    MiPerfilResponse consultar(Long id) {
        E persona = obtener.apply(id);
        return mapeo.aMiPerfil(persona, numeroSocioDe.apply(persona));
    }

    MiPerfilResponse actualizar(Long id, PersonaUpdateRequest cambios) {
        return actualizar(id, cambios, null, null);
    }

    MiPerfilResponse actualizar(Long id, PersonaUpdateRequest cambios, Sexo sexo, String localidad) {
        E persona = obtener.apply(id);
        asegurarQueNoChoca.accept(persona, cambios);

        mapeo.aplicar(cambios, persona);
        if (sexo != null) {
            persona.setSexo(sexo);
        }
        if (localidad != null) {
            persona.setLocalidad(localidad);
        }
        E guardada = guardar.apply(persona);
        return mapeo.aMiPerfil(guardada, numeroSocioDe.apply(guardada));
    }

    void cambiarPassword(Long id, String passwordActual, String passwordNueva) {
        E persona = obtener.apply(id);
        if (!passwordEncoder.matches(passwordActual, persona.getPasswordHash())) {
            throw new ReglaDeNegocioException(CodigoError.CONTRASENIA_ACTUAL_INCORRECTA,
                    "La contraseña actual no es correcta", Map.of("passwordActual", "La contraseña actual no es correcta"));
        }
        if (passwordNueva.equals(passwordActual)) {
            throw new ReglaDeNegocioException(CodigoError.CONTRASENIA_IGUAL_A_LA_ACTUAL,
                    "La contraseña nueva tiene que ser distinta de la actual",
                    Map.of("passwordNueva", "Tiene que ser distinta de la actual"));
        }
        GestionDePersonas.validarContrasenia(passwordNueva);

        persona.setPasswordHash(passwordEncoder.encode(passwordNueva));
        guardar.apply(persona);
        log.info("Persona {} cambió su contraseña", id);
    }

    String actualizarFoto(Long id, String url) {
        E persona = obtener.apply(id);
        persona.setFotoUrl(url);
        guardar.apply(persona);
        return url;
    }

    void quitarFoto(Long id) {
        E persona = obtener.apply(id);
        persona.setFotoUrl(null);
        guardar.apply(persona);
    }
}
