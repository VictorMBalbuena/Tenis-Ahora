package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.cuenta.RegistroPorRol;
import com.tenisahora.shared.model.Rol;
import org.springframework.stereotype.Component;

@Component
public class RegistroDeProfesores extends RegistroPorRol<Profesor> {

    public RegistroDeProfesores(ProfesorRepository repositorio) {
        super(repositorio, Rol.PROFESOR);
    }

    @Override
    protected Profesor nueva() {
        return new Profesor();
    }
}
