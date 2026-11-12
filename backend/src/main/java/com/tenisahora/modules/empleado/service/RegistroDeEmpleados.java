package com.tenisahora.modules.empleado.service;

import com.tenisahora.modules.empleado.entity.Empleado;
import com.tenisahora.modules.empleado.repository.EmpleadoRepository;
import com.tenisahora.shared.cuenta.RegistroPorRol;
import com.tenisahora.shared.model.Rol;
import org.springframework.stereotype.Component;

@Component
public class RegistroDeEmpleados extends RegistroPorRol<Empleado> {

    public RegistroDeEmpleados(EmpleadoRepository repositorio) {
        super(repositorio, Rol.EMPLEADO);
    }

    @Override
    protected Empleado nueva() {
        return new Empleado();
    }
}
