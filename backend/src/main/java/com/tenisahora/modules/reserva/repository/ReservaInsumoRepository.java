package com.tenisahora.modules.reserva.repository;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ReservaInsumoRepository extends JpaRepository<ReservaInsumo, Long> {

    /** Por insumo: [id, unidades en uso que se descontaron al iniciar el alquiler y todavía no se repusieron]. */
    @Query("select ri.insumo.id, sum(ri.cantidadAsignada) from ReservaInsumo ri join ri.reserva r "
            + "where ri.insumo.id in :insumoIds and ri.descontado = true and ri.repuesto = false "
            + "and r.estado = com.tenisahora.modules.reserva.entity.EstadoReserva.EN_CURSO group by ri.insumo.id")
    List<Object[]> sumaEnUso(Collection<Long> insumoIds);

    /** Ids de las reservas de los estados indicados, de la fecha en adelante, que tienen asignado el insumo. */
    @Query("select r.id from ReservaInsumo ri join ri.reserva r where ri.insumo.id = :insumoId "
            + "and r.estado in :estados and r.fecha >= :desde group by r.id order by r.id")
    List<Long> reservasConInsumo(Long insumoId, Collection<EstadoReserva> estados, LocalDate desde);

    /** Suelta los insumos de la reserva (al cancelarla): quedan como repuestos sin tocar el stock, porque todavía no se habían descontado. */
    @Modifying(flushAutomatically = true)
    @Query("update ReservaInsumo ri set ri.repuesto = true where ri.reserva.id = :reservaId and ri.repuesto = false")
    int liberarDeReserva(Long reservaId);

    /** Los insumos de una reserva, ordenados por insumo (el mismo orden en que se bloquean y se mueven en el stock). */
    @Query("select ri from ReservaInsumo ri join fetch ri.insumo where ri.reserva.id = :reservaId order by ri.insumo.id")
    List<ReservaInsumo> deLaReserva(Long reservaId);

    @Query("select ri from ReservaInsumo ri join fetch ri.insumo where ri.reserva.id in :reservaIds order by ri.reserva.id, ri.insumo.id")
    List<ReservaInsumo> findByReservaIdIn(Collection<Long> reservaIds);

    /** Borra los insumos de la reserva: el pedido nuevo los reemplaza. */
    @Modifying(flushAutomatically = true)
    @Query("delete from ReservaInsumo ri where ri.reserva.id = :reservaId")
    int borrarDeLaReserva(Long reservaId);

    /**
     * Por insumo: [id, unidades comprometidas y no repuestas] a reservas vigentes de ese día cuyo horario se pisa con el turno
     * (la reserva excluida no cuenta: sirve al reasignar).
     */
    @Query("select ri.insumo.id, sum(ri.cantidadAsignada) from ReservaInsumo ri join ri.reserva r "
            + "where ri.insumo.id in :insumoIds and ri.repuesto = false and r.estado in :estados "
            + "and r.fecha = :fecha and r.horaInicio < :horaHasta and r.horaFin > :horaDesde and r.id <> :excluirId group by ri.insumo.id")
    List<Object[]> comprometidos(Collection<Long> insumoIds, Collection<EstadoReserva> estados, LocalDate fecha,
                                 LocalTime horaDesde, LocalTime horaHasta, Long excluirId);

    /** Marca como descontados los insumos de la reserva (se hace después de sacarlos del stock). */
    @Modifying(flushAutomatically = true)
    @Query("update ReservaInsumo ri set ri.descontado = true where ri.reserva.id = :reservaId and ri.descontado = false and ri.repuesto = false")
    int marcarDescontados(Long reservaId);

    /** Marca como repuestos los insumos de la reserva que estaban en uso (se hace después de devolverlos al stock). */
    @Modifying(flushAutomatically = true)
    @Query("update ReservaInsumo ri set ri.repuesto = true where ri.reserva.id = :reservaId and ri.descontado = true and ri.repuesto = false")
    int marcarRepuestos(Long reservaId);
}
