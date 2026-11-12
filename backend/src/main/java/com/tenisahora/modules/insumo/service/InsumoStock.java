package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.MovimientoRequest;
import com.tenisahora.modules.insumo.dto.MovimientoResponse;
import com.tenisahora.shared.dto.PageResponse;

/**
 * Movimientos de stock. Los usa el endpoint de movimientos y, más adelante, las reservas (BACK 07) para descontar y
 * reponer solas. Cada operación es atómica: el stock nunca queda negativo.
 */
public interface InsumoStock {

    /** CU14: resta unidades; si no alcanzan, falla y el stock no cambia. */
    InsumoResponse descontar(Long insumoId, int cantidad, String motivo, Long personaId);

    /** CU15: suma unidades. */
    InsumoResponse reponer(Long insumoId, int cantidad, String motivo, Long personaId);

    /** Descontar o reponer según el pedido. */
    InsumoResponse mover(Long insumoId, MovimientoRequest request, Long personaId);

    /** Rastro de movimientos del insumo, del más reciente al más antiguo. */
    PageResponse<MovimientoResponse> historial(Long insumoId, int page, int size);
}
