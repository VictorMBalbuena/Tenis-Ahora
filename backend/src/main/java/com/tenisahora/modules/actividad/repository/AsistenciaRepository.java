package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    /** Las marcas de una fecha, con el socio ya traído: para CU42/CU43 y para armar la respuesta sin N+1. */
    @Query("select a from Asistencia a join fetch a.socio where a.actividad.id = :actividadId and a.fecha = :fecha")
    List<Asistencia> deLaActividadEnLaFecha(Long actividadId, LocalDate fecha);

    /** Registrar dos veces la misma marca corrige en vez de duplicar (la UK es la red de seguridad). */
    Optional<Asistencia> findByActividadIdAndSocioIdAndFecha(Long actividadId, Long socioId, LocalDate fecha);

    /** El historial de un socio en el rango, con la actividad ya traída: para el futuro reporte de asistencias (CU68). */
    @Query("select a from Asistencia a join fetch a.actividad where a.socio.id = :socioId and a.fecha between :desde and :hasta order by a.fecha")
    List<Asistencia> delSocioEntre(Long socioId, LocalDate desde, LocalDate hasta);

    /** Cuántas veces estuvo presente en una actividad dentro del rango: para el futuro reporte de asistencias (CU68). */
    @Query("select count(a) from Asistencia a where a.actividad.id = :actividadId and a.presente = true and a.fecha between :desde and :hasta")
    long contarPresentes(Long actividadId, LocalDate desde, LocalDate hasta);
}
