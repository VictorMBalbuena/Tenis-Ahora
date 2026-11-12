package com.tenisahora.modules.torneo.controller;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.model.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

final class ActorDeSesion {

    private static final String PREFIJO_DE_ROL = "ROLE_";

    private ActorDeSesion() {
    }

    static Actor de(Authentication autenticacion) {
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Long)) {
            return null;
        }
        Rol rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith(PREFIJO_DE_ROL))
                .map(autoridad -> Rol.valueOf(autoridad.substring(PREFIJO_DE_ROL.length())))
                .findFirst()
                .orElse(null);
        if (rol == null) return null;
        return new Actor((Long) autenticacion.getPrincipal(), rol);
    }
}
