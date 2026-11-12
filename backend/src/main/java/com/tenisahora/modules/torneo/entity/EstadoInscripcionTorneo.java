package com.tenisahora.modules.torneo.entity;

/** La inscripción queda firme al pagar el arancel; sin pagar en plazo, se cancela sola. */
public enum EstadoInscripcionTorneo {
    PENDIENTE_PAGO,
    CONFIRMADA,
    CANCELADA
}
