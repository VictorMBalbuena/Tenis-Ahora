package com.tenisahora.modules.reserva.dto;

/** En qué punto está un insumo de una reserva: ASIGNADO todavía no se descontó, EN_USO ya salió del stock y REPUESTO volvió o se liberó. */
public enum EstadoDeAsignacion {
    ASIGNADO,
    EN_USO,
    REPUESTO
}
