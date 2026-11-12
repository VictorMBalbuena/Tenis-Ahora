package com.tenisahora.modules.cancha.dto;

/** Estado visible de una cancha en el listado (CU08): deriva de activa + si tiene un bloqueo vigente. */
public enum EstadoCancha {
    DISPONIBLE,
    MANTENIMIENTO,
    DESHABILITADA
}
