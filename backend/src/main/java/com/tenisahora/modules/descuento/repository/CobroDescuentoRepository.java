package com.tenisahora.modules.descuento.repository;

import com.tenisahora.modules.descuento.entity.CobroDescuento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CobroDescuentoRepository extends JpaRepository<CobroDescuento, Long> {

    long countByDescuentoId(Long descuentoId);

    boolean existsByDescuentoId(Long descuentoId);

    /** Para no registrar dos veces el mismo descuento sobre el mismo cobro si se recalcula. */
    boolean existsByCobroIdAndDescuentoId(Long cobroId, Long descuentoId);
}

