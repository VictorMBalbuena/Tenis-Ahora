package com.tenisahora.modules.insumo.repository;

import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaInsumoRepository extends JpaRepository<CategoriaInsumo, Long> {

    List<CategoriaInsumo> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
