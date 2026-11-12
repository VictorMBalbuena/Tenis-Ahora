package com.tenisahora.modules.insumo.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Puerto: lo que el módulo de insumos necesita saber de las reservas. Lo implementa el módulo reserva, así insumos no
 * depende de él (las reservas ya dependen de los insumos).
 */
public interface UsoDeInsumos {

    /** Unidades de cada insumo asignadas a reservas que todavía no se repusieron. Los que no figuran no están en uso. */
    Map<Long, Integer> enUso(Collection<Long> insumoIds);

    /** Ids de las reservas CONFIRMADAS o pendientes de pago, de la fecha en adelante, que tienen asignado el insumo. */
    List<Long> reservasVigentesCon(Long insumoId, LocalDate desde);
}
