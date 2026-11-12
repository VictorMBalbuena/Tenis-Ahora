package com.tenisahora.modules.cancha.repository;

import com.tenisahora.modules.cancha.entity.Disponibilidad;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    List<Disponibilidad> findByCanchaIdAndEstadoAndFechaBetween(Long canchaId, EstadoDisponibilidad estado,
                                                                LocalDate desde, LocalDate hasta);

    Optional<Disponibilidad> findByIdAndCanchaIdAndEstado(Long id, Long canchaId, EstadoDisponibilidad estado);

    /** Ids de las canchas con un bloqueo que cubre ese día y esa hora (estado de mantenimiento actual). */
    @Query("select distinct d.cancha.id from Disponibilidad d where d.cancha.id in :canchaIds and d.estado = :estado "
            + "and d.fecha = :fecha and d.horaInicio <= :hora and d.horaFin > :hora")
    Set<Long> canchasBloqueadasEn(Collection<Long> canchaIds, EstadoDisponibilidad estado, LocalDate fecha, LocalTime hora);
}
