package com.tenisahora.modules.insumo.dto;

/** Topes para que un dato mal cargado no desborde el stock ni arme consultas enormes. */
public final class LimitesDeStock {

    public static final int MAXIMO_STOCK = 10_000_000;
    public static final int MAXIMO_POR_MOVIMIENTO = 100_000;

    private LimitesDeStock() {
    }
}
