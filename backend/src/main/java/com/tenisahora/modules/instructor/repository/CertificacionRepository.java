package com.tenisahora.modules.instructor.repository;

import com.tenisahora.modules.instructor.entity.Certificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {

    /** El número de certificación es único entre todos los instructores. */
    boolean existsByNumeroCertificacionIgnoreCase(String numeroCertificacion);

    Optional<Certificacion> findByNumeroCertificacionIgnoreCase(String numeroCertificacion);

    boolean existsByNumeroCertificacionIgnoreCaseAndIdNot(String numeroCertificacion, Long id);
}
