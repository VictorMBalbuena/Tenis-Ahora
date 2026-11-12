package com.tenisahora.modules.instructor.entity;

import com.tenisahora.shared.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profesor")
@Getter
@Setter
@NoArgsConstructor
public class Profesor extends Instructor {

    @Column(name = "titulo_habilitante", length = 150)
    private String tituloHabilitante;

    @Override
    public Rol getRol() {
        return Rol.PROFESOR;
    }
}
