package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.Entrenamiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long>, JpaSpecificationExecutor<Entrenamiento> {

    /** Los entrenamientos del entrenador, pasados y vigentes, para CU83 (mis entrenamientos). */
    Page<Entrenamiento> findByEntrenadorId(Long entrenadorId, Pageable pageable);
}
