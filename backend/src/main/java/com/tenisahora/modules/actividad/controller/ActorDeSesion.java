package com.tenisahora.modules.actividad.controller;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Arma el actor a partir de la sesión ya validada por el filtro JWT (el rol que vale es el de la base, no el del token). */
final class ActorDeSesion {

    private static final String PREFIJO_DE_ROL = "ROLE_";

    private ActorDeSesion() {
    }

    static Actor de(Authentication autenticacion) {
        Rol rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith(PREFIJO_DE_ROL))
                .map(autoridad -> Rol.valueOf(autoridad.substring(PREFIJO_DE_ROL.length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La sesión no tiene rol"));
        return new Actor((Long) autenticacion.getPrincipal(), rol);
    }
}
