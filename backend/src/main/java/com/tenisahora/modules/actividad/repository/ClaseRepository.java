package com.tenisahora.modules.actividad.repository;

import com.tenisahora.modules.actividad.entity.Clase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClaseRepository extends JpaRepository<Clase, Long>, JpaSpecificationExecutor<Clase> {

    /** Las clases del profesor, pasadas y vigentes, para CU81 (mis clases). */
    Page<Clase> findByProfesorId(Long profesorId, Pageable pageable);
}
