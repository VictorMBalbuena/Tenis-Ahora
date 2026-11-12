package com.tenisahora.modules.cobro.repository;

import com.tenisahora.modules.cobro.entity.Recibo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReciboRepository extends JpaRepository<Recibo, Long>, JpaSpecificationExecutor<Recibo> {

    /** La emisión del recibo es idempotente: si el cobro ya tiene uno, no se emite otro (CU57). */
    boolean existsByCobroId(Long cobroId);

    Optional<Recibo> findByCobroId(Long cobroId);

    List<Recibo> findByCobroIdIn(Collection<Long> cobroIds);
}
