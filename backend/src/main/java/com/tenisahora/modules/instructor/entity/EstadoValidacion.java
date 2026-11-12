package com.tenisahora.modules.instructor.entity;

public enum EstadoValidacion {
    PENDIENTE,
    VALIDADO,
    RECHAZADA;

    /** Se puede decidir sobre una pendiente, y también revisar de nuevo una rechazada si cambió el título (CU31). */
    public boolean puedeValidarse() {
        return this == PENDIENTE || this == RECHAZADA;
    }

    /** RD02 y RD03: solo un instructor con la certificación aprobada se asigna a clases o entrenamientos. */
    public boolean puedeAsignarse() {
        return this == VALIDADO;
    }

    public boolean fueRechazada() {
        return this == RECHAZADA;
    }
}
