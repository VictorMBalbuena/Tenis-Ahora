package com.tenisahora.modules.administrador.entity;

import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "administrador")
@Getter
@Setter
@NoArgsConstructor
public class Administrador extends Persona {

    @Column(name = "es_administrador", nullable = false)
    private Boolean esAdministrador = true;

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }
}
