package com.tenisahora.modules.administrador.repository;

import com.tenisahora.modules.administrador.entity.Administrador;
import com.tenisahora.shared.persona.RepositorioDePersonas;

public interface AdministradorRepository extends RepositorioDePersonas<Administrador> {

    /** Para no dejar al club sin quien lo administre. */
    long countByActivoTrue();
}
