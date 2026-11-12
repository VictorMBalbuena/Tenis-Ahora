package com.tenisahora.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Lo que tienen en común los empleados, los profesores y los entrenadores (todos son parte del personal del club).
 * Sin tabla propia: cada uno guarda estos datos en la suya.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class EmpleadoBase extends Persona {

    @Column(name = "fecha_contratacion")
    private LocalDate fechaContratacion;

    @Column(name = "permiso_personal_autorizado")
    private Boolean permisoPersonalAutorizado = false;
}
