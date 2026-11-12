package com.tenisahora.modules.cancha.repository;

import com.tenisahora.modules.cancha.entity.TipoCancha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoCanchaRepository extends JpaRepository<TipoCancha, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<TipoCancha> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<TipoCancha> findAllByActivoTrueOrderByNombreAsc();

    Optional<TipoCancha> findByIdAndActivoTrue(Long id);
}

