package com.tenisahora.modules.actividad.entity;

/** Una inscripción retiene el lugar mientras espera la seña, igual que una reserva retiene el turno. */
public enum EstadoInscripcion {
    PENDIENTE_PAGO,
    CONFIRMADA,
    CANCELADA
}
