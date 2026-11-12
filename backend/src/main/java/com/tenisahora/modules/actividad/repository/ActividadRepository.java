package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /** Para modificar o inscribir: bloquea la fila hasta terminar, así dos operaciones sobre la misma actividad se atienden de a una. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Actividad a where a.id = :id")
    Optional<Actividad> buscarParaModificar(Long id);

    /**
     * Hay otra actividad (de los estados indicados) en la misma cancha cuyo rango de fechas se cruza, que comparte
     * al menos uno de los días de la semana indicados y cuyo horario se pisa con la franja. La actividad en
     * excluirId no cuenta (sirve al modificar).
     */
    @Query("select count(a) > 0 from Actividad a join ActividadDia d on d.actividad = a "
            + "where a.cancha.id = :canchaId and a.estado in :estados and a.id <> :excluirId "
            + "and a.fechaInicio <= :hasta and a.fechaFin >= :desde and d.diaSemana in :dias "
            + "and a.horaInicio < :horaHasta and a.horaFin > :horaDesde")
    boolean haySuperposicion(Long canchaId, LocalDate desde, LocalDate hasta, Collection<DayOfWeek> dias,
                             LocalTime horaDesde, LocalTime horaHasta, Collection<EstadoActividad> estados, Long excluirId);

    /** Actividades (de los estados indicados) de la cancha cuyo rango de fechas se cruza con el pedido. Para armar la grilla de disponibilidad. */
    @Query("select distinct a from Actividad a where a.cancha.id = :canchaId and a.estado in :estados "
            + "and a.fechaInicio <= :hasta and a.fechaFin >= :desde")
    List<Actividad> vigentesEnRango(Long canchaId, LocalDate desde, LocalDate hasta, Collection<EstadoActividad> estados);

    /** Ids de las actividades (de los estados indicados) que, en la fecha indicada, caen en ese día de la semana y se pisan con la franja. */
    @Query("select distinct a.id from Actividad a join ActividadDia d on d.actividad = a "
            + "where a.cancha.id = :canchaId and a.estado in :estados and d.diaSemana = :dia "
            + "and a.fechaInicio <= :fecha and a.fechaFin >= :fecha "
            + "and a.horaInicio < :horaHasta and a.horaFin > :horaDesde")
    List<Long> idsVigentesEnElDia(Long canchaId, LocalDate fecha, DayOfWeek dia, LocalTime horaDesde, LocalTime horaHasta,
                                  Collection<EstadoActividad> estados);

    /**
     * El profesor ya tiene otra actividad (de los estados indicados, salvo excluirId) cuyo rango se cruza, que
     * comparte alguno de los días y cuyo horario se pisa con la franja. Para CU44: saber quién está libre.
     */
    @Query("select count(a) > 0 from Actividad a join ActividadDia d on d.actividad = a "
            + "where a.profesor.id = :profesorId and a.estado in :estados and a.id <> :excluirId "
            + "and a.fechaInicio <= :hasta and a.fechaFin >= :desde and d.diaSemana in :dias "
            + "and a.horaInicio < :horaHasta and a.horaFin > :horaDesde")
    boolean profesorConSuperposicion(Long profesorId, LocalDate desde, LocalDate hasta, Collection<DayOfWeek> dias,
                                     LocalTime horaDesde, LocalTime horaHasta, Collection<EstadoActividad> estados, Long excluirId);

    /** Lo mismo que {@link #profesorConSuperposicion}, para el entrenador. */
    @Query("select count(a) > 0 from Actividad a join ActividadDia d on d.actividad = a "
            + "where a.entrenador.id = :entrenadorId and a.estado in :estados and a.id <> :excluirId "
            + "and a.fechaInicio <= :hasta and a.fechaFin >= :desde and d.diaSemana in :dias "
            + "and a.horaInicio < :horaHasta and a.horaFin > :horaDesde")
    boolean entrenadorConSuperposicion(Long entrenadorId, LocalDate desde, LocalDate hasta, Collection<DayOfWeek> dias,
                                       LocalTime horaDesde, LocalTime horaHasta, Collection<EstadoActividad> estados, Long excluirId);
}
