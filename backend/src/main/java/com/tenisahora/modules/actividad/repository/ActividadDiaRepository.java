package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.ActividadDia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ActividadDiaRepository extends JpaRepository<ActividadDia, Long> {

    List<ActividadDia> findByActividadId(Long actividadId);

    /** Los días de varias actividades de una sola consulta, para armar respuestas sin N+1. */
    List<ActividadDia> findByActividadIdIn(Collection<Long> actividadIds);

    void deleteByActividadId(Long actividadId);
}
