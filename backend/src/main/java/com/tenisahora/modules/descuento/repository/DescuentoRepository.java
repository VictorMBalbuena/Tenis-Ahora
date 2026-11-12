package com.tenisahora.modules.descuento.repository;

import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DescuentoRepository extends JpaRepository<Descuento, Long>, JpaSpecificationExecutor<Descuento> {

    List<Descuento> findByActivoTrue();

    /** El código se guarda en mayúsculas, pero se busca sin distinguirlas por si llega de otra forma. */
    Optional<Descuento> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByTipoAndActivoTrue(TipoDescuento tipo);

    boolean existsByTipoAndActivoTrueAndIdNot(TipoDescuento tipo, Long id);
}

