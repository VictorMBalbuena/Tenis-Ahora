package com.tenisahora.modules.cancha.repository;

import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioSemanalRepository extends JpaRepository<HorarioSemanal, Long> {

    List<HorarioSemanal> findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(Long canchaId);

    Optional<HorarioSemanal> findByIdAndCanchaId(Long id, Long canchaId);

    boolean existsByCanchaId(Long canchaId);

    /** Dos franjas se pisan si cada una empieza antes de que termine la otra: 8-10 y 10-12 no se pisan. */
    @Query("select count(h) > 0 from HorarioSemanal h where h.cancha.id = :canchaId and h.diaSemana = :dia "
            + "and h.horaInicio < :fin and h.horaFin > :inicio")
    boolean haySolapamiento(Long canchaId, DayOfWeek dia, LocalTime inicio, LocalTime fin);
}
