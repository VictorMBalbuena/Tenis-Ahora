package com.tenisahora.modules.actividad.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ENTRENAMIENTO")
@Getter
@Setter
@NoArgsConstructor
public class Entrenamiento extends Actividad {

    private String objetivo;
}
