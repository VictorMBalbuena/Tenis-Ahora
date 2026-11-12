package com.tenisahora.modules.actividad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CLASE")
@Getter
@Setter
@NoArgsConstructor
public class Clase extends Actividad {

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelClase nivel;
}
