package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.entity.Insumo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Lo que otros módulos (reservas) necesitan saber del stock libre de los insumos, sin conocer cómo se guarda. */
public interface ConsultaDeStock {

    /** Unidades de un insumo: libres en depósito y total perteneciente al club (depósito + en uso). */
    record StockLibre(Insumo insumo, int cantidadLibre, int stockTotal) {
        public StockLibre(Insumo insumo, int cantidadLibre) {
            this(insumo, cantidadLibre, cantidadLibre);
        }
    }

    /**
     * Bloquea las filas de los insumos hasta terminar la transacción (en orden de id) y devuelve su stock libre. Falla con
     * 404 si alguno no existe o está dado de baja.
     */
    Map<Long, StockLibre> bloquear(Collection<Long> insumoIds);

    /** El stock libre de todos los insumos activos, por nombre. */
    List<StockLibre> activos();
}
