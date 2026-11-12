package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.ReservaResponse;

/** El alquiler de una cancha: arranca (CU14, descuenta el stock) y termina (CU15, lo repone). */
public interface CicloDelAlquiler {

    /** Registra el inicio: la reserva pasa a EN_CURSO y los insumos asignados salen del stock. Todo o nada. */
    ReservaResponse iniciar(Long reservaId, Actor actor);

    /** Registra la finalización: la reserva pasa a FINALIZADA y los insumos descontados vuelven al stock. */
    ReservaResponse finalizar(Long reservaId, Actor actor);

    /** Finaliza solas las reservas en curso cuyo turno terminó hace más del margen. Devuelve cuántas finalizó. */
    int finalizarVencidas();
}
