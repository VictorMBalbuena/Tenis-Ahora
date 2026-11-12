package com.tenisahora.modules.insumo.repository;

import com.tenisahora.modules.insumo.entity.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    Page<MovimientoStock> findByInsumoIdOrderByFechaDescIdDesc(Long insumoId, Pageable pageable);
}
