package com.tenisahora.modules.torneo.repository;

import com.tenisahora.modules.torneo.entity.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    Optional<Torneo> findFirstByNombre(String nombre);
}
