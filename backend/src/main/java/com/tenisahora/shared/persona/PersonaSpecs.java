package com.tenisahora.shared.persona;

import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.util.Textos;
import org.springframework.data.jpa.domain.Specification;

/** Filtros de búsqueda comunes a todos los roles. */
public final class PersonaSpecs {

    private static final char ESCAPE = '!';

    private PersonaSpecs() {
    }

    public static <E extends Persona> Specification<E> conDni(String dni) {
        return (persona, consulta, cb) -> cb.equal(persona.get("dni"), dni);
    }

    /** El patrón ya viene sin tildes, en minúsculas y con los comodines escapados (ver {@link Textos#patronContiene}). */
    public static <E extends Persona> Specification<E> conNombreQueContiene(String patron) {
        return (persona, consulta, cb) -> {
            var nombreCompleto = cb.concat(cb.concat(persona.<String>get("nombre"), " "), persona.<String>get("apellido"));
            var sinTildes = cb.function("translate", String.class, nombreCompleto,
                    cb.literal(Textos.LETRAS_CON_TILDE), cb.literal(Textos.LETRAS_SIN_TILDE));
            var coincideNombre = cb.like(cb.lower(sinTildes), patron, ESCAPE);
            var coincideEmail = cb.like(cb.lower(persona.get("email")), patron, ESCAPE);
            var coincideDni = cb.like(persona.get("dni"), patron, ESCAPE);
            return cb.or(coincideNombre, coincideEmail, coincideDni);
        };
    }
}
