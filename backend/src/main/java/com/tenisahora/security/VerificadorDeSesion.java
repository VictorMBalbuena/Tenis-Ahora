package com.tenisahora.security;

import java.util.Optional;

/**
 * Puerto que el filtro JWT usa para saber si la sesión de un token sigue vigente. Lo implementa el módulo auth,
 * así security no depende de ningún módulo.
 */
public interface VerificadorDeSesion {

    /**
     * @param rolDelToken el rol con el que se emitió el token: indica en qué tabla buscar la cuenta
     * @return el rol actual si la cuenta existe, sigue siendo de ese rol y está activa; vacío si no
     */
    Optional<String> rolVigente(Long personaId, String rolDelToken);
}
