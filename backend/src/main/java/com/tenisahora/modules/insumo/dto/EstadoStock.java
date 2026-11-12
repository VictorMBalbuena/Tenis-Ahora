package com.tenisahora.modules.insumo.dto;

/** Se deduce de la cantidad, no se guarda: SIN_STOCK si hay 0, STOCK_BAJO si hay hasta el mínimo y DISPONIBLE si hay más. */
public enum EstadoStock {
    DISPONIBLE,
    STOCK_BAJO,
    SIN_STOCK
}
