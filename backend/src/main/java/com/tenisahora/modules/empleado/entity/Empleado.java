package com.tenisahora.modules.empleado.entity;

import com.tenisahora.shared.entity.EmpleadoBase;
import com.tenisahora.shared.model.Rol;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empleado")
@NoArgsConstructor
public class Empleado extends EmpleadoBase {

    @Override
    public Rol getRol() {
        return Rol.EMPLEADO;
    }
}
