package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.cuenta.RegistroPorRol;
import com.tenisahora.shared.model.Rol;
import org.springframework.stereotype.Component;

@Component
public class RegistroDeEntrenadores extends RegistroPorRol<Entrenador> {

    public RegistroDeEntrenadores(EntrenadorRepository repositorio) {
        super(repositorio, Rol.ENTRENADOR);
    }

    @Override
    protected Entrenador nueva() {
        return new Entrenador();
    }
}
