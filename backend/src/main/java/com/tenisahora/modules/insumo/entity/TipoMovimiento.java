package com.tenisahora.modules.insumo.entity;

/** Por qué cambió el stock: DESCUENTO (CU14) y REPOSICION (CU15) los pide el empleado o una reserva; AJUSTE es la corrección manual de CU12. */
public enum TipoMovimiento {
    DESCUENTO,
    REPOSICION,
    AJUSTE
}
