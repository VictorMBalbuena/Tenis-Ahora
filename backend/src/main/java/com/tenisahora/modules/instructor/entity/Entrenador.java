package com.tenisahora.modules.instructor.entity;

import com.tenisahora.shared.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entrenador")
@Getter
@Setter
@NoArgsConstructor
public class Entrenador extends Instructor {

    @Column(name = "certificacion_deportiva", length = 150)
    private String certificacionDeportiva;

    @Override
    public Rol getRol() {
        return Rol.ENTRENADOR;
    }
}
