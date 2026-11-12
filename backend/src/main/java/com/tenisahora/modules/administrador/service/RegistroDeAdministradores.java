package com.tenisahora.modules.administrador.service;

import com.tenisahora.modules.administrador.entity.Administrador;
import com.tenisahora.modules.administrador.repository.AdministradorRepository;
import com.tenisahora.shared.cuenta.RegistroPorRol;
import com.tenisahora.shared.model.Rol;
import org.springframework.stereotype.Component;

@Component
public class RegistroDeAdministradores extends RegistroPorRol<Administrador> {

    public RegistroDeAdministradores(AdministradorRepository repositorio) {
        super(repositorio, Rol.ADMINISTRADOR);
    }

    @Override
    protected Administrador nueva() {
        return new Administrador();
    }
}
