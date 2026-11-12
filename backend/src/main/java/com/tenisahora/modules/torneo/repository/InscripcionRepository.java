package com.tenisahora.modules.torneo.repository;

import com.tenisahora.modules.torneo.entity.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    /** Las vigentes: una inscripción cancelada no ocupa lugar ni cuenta para el cuadro. */
    @Query("SELECT i FROM Inscripcion i WHERE i.torneo.id = :torneoId AND i.estado <> com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo.CANCELADA ORDER BY i.equipo")
    List<Inscripcion> findByTorneoId(@Param("torneoId") Long torneoId);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.torneo WHERE i.jugador1.id = :socioId OR i.pareja.id = :socioId ORDER BY i.id DESC")
    List<Inscripcion> delSocio(@Param("socioId") Long socioId);

    @Query("SELECT i FROM Inscripcion i WHERE i.estado = com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo.PENDIENTE_PAGO AND i.fechaCreacion < :limite")
    List<Inscripcion> pendientesCreadasAntesDe(@Param("limite") LocalDateTime limite);

    @Query("SELECT COUNT(i) > 0 FROM Inscripcion i WHERE i.torneo.id = :torneoId AND (i.jugador1.id = :socioId OR i.pareja.id = :socioId) AND i.estado <> com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo.CANCELADA")
    boolean existsSocioEnTorneo(@Param("torneoId") Long torneoId, @Param("socioId") Long socioId);
}
