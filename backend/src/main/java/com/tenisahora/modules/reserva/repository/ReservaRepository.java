package com.tenisahora.modules.reserva.repository;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    List<Reserva> findByCanchaIdAndEstadoInAndFechaBetween(Long canchaId, Collection<EstadoReserva> estados,
                                                           LocalDate desde, LocalDate hasta);

    /** Ids de las reservas de los estados indicados que se pisan con la franja diaria entre las dos fechas. */
    @Query("select r.id from Reserva r where r.cancha.id = :canchaId and r.estado in :estados "
            + "and r.fecha between :desde and :hasta and r.horaInicio < :horaHasta and r.horaFin > :horaDesde order by r.id")
    List<Long> idsPorEstadosEnRango(Long canchaId, Collection<EstadoReserva> estados, LocalDate desde, LocalDate hasta,
                                   LocalTime horaDesde, LocalTime horaHasta);

    /** Reservas de los estados indicados desde la fecha en adelante que se pisan con la franja diaria. */
    @Query("select r from Reserva r where r.cancha.id = :canchaId and r.estado in :estados and r.fecha >= :desde "
            + "and r.horaInicio < :horaHasta and r.horaFin > :horaDesde order by r.id")
    List<Reserva> vigentesDesde(Long canchaId, Collection<EstadoReserva> estados, LocalDate desde,
                                LocalTime horaDesde, LocalTime horaHasta);

    /** Hay otra reserva de los estados indicados que se pisa con el turno (la indicada en excluirId no cuenta: sirve al modificar). */
    @Query("select count(r) > 0 from Reserva r where r.cancha.id = :canchaId and r.estado in :estados and r.fecha = :fecha "
            + "and r.horaInicio < :horaHasta and r.horaFin > :horaDesde and r.id <> :excluirId")
    boolean haySuperposicion(Long canchaId, Collection<EstadoReserva> estados, LocalDate fecha, LocalTime horaDesde,
                             LocalTime horaHasta, Long excluirId);

    /** Reservas en ese estado creadas antes del límite: las pendientes de pago que ya vencieron. */
    @Query("select r from Reserva r where r.estado = :estado and coalesce(r.plazoPagoDesde, r.fechaCreacion) < :limite order by r.id")
    List<Reserva> creadasAntesDe(EstadoReserva estado, LocalDateTime limite);

    /** Para modificar o cancelar: bloquea la fila hasta terminar, así dos operaciones sobre la misma reserva se atienden de a una. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> buscarParaModificar(Long id);

    /** Reservas en ese estado cuyo turno terminó en la fecha y hora límite o antes: el alquiler que nadie cerró. */
    @Query("select r from Reserva r where r.estado = :estado and (r.fecha < :fechaLimite or (r.fecha = :fechaLimite and r.horaFin <= :horaLimite)) order by r.id")
    List<Reserva> terminadasAntesDe(EstadoReserva estado, LocalDate fechaLimite, LocalTime horaLimite);
}
