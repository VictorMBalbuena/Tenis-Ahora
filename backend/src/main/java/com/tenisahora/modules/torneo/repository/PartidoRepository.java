package com.tenisahora.modules.torneo.repository;

import com.tenisahora.modules.torneo.entity.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    List<Partido> findByTorneoIdOrderByRondaAscOrdenAsc(Long torneoId);

    Optional<Partido> findByTorneoIdAndRondaAndOrden(Long torneoId, Integer ronda, Integer orden);
}
