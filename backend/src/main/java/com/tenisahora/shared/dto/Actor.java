package com.tenisahora.shared.dto;

import com.tenisahora.shared.model.Rol;

/** Quién hace la operación (sale del token ya validado): sirve para las reglas de permisos del negocio. */
public record Actor(Long id, Rol rol) {

    public boolean esAdministrador() {
        return rol == Rol.ADMINISTRADOR;
    }
}
