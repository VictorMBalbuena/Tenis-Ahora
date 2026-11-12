package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.CancelacionResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;

public interface CancelacionService {

    /** CU18 A1: qué pasaría al cancelar ahora (a tiempo o tardía, cargo y reembolso), sin cambiar nada. */
    CancelacionResponse previsualizar(Long id, Actor actor);

    /** CU18: cancela, aplica la política, libera el horario y los insumos asignados. Los insumos no tocan el stock. */
    ReservaResponse cancelar(Long id, Actor actor);

    /** Cancela las pendientes de pago que vencieron, sin cargo, y libera su horario. Devuelve cuántas fueron. */
    int cancelarVencidas();
}
