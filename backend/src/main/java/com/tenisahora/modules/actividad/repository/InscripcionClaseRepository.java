package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionClaseRepository extends JpaRepository<InscripcionClase, Long> {

    long countByActividadIdAndActivaTrue(Long actividadId);

    Optional<InscripcionClase> findByActividadIdAndSocioId(Long actividadId, Long socioId);

    boolean existsByActividadIdAndActivaTrue(Long actividadId);

    @Query("select i from InscripcionClase i join fetch i.actividad join fetch i.socio where i.socio.id = :socioId order by i.id desc")
    List<InscripcionClase> delSocio(Long socioId);

    @Query("select i from InscripcionClase i join fetch i.actividad join fetch i.socio where i.id = :id")
    Optional<InscripcionClase> conActividad(Long id);

    @Query("select i from InscripcionClase i where i.estado = :estado and i.fechaCreacion < :limite")
    List<InscripcionClase> pendientesCreadasAntesDe(EstadoInscripcion estado, LocalDateTime limite);

    /** Los inscriptos activos de la clase, con el socio ya traído: evita el N+1 al listarlos. */
    @Query("select i from InscripcionClase i join fetch i.socio where i.actividad.id = :actividadId and i.activa = true "
            + "order by i.fechaInscripcion, i.id")
    List<InscripcionClase> deLaActividad(Long actividadId);

    /** La cantidad de inscriptos activos de varias actividades, de una sola consulta: para no hacer N+1 al listar clases. */
    @Query("select i.actividad.id as actividadId, count(i) as cantidad from InscripcionClase i "
            + "where i.actividad.id in :actividadIds and i.activa = true group by i.actividad.id")
    List<ConteoDeInscriptos> contarActivasPorActividad(Collection<Long> actividadIds);
}
