package com.tenisahora.shared.cuenta;

import com.tenisahora.shared.model.Rol;

/** Lo mínimo de una cuenta que necesita auth para iniciar y mantener una sesión. No es una entidad. */
public record CuentaDeAcceso(Long id, String email, String passwordHash, Rol rol, boolean activa,
                             Long rolPersonalizadoId) {

    /** La mayoría de las veces la cuenta no tiene un rol personalizado: usa los permisos de su rol base. */
    public CuentaDeAcceso(Long id, String email, String passwordHash, Rol rol, boolean activa) {
        this(id, email, passwordHash, rol, activa, null);
    }
}
