package com.tenisahora.modules.permiso.repository;

import com.tenisahora.modules.permiso.entity.RolDelSistema;
import com.tenisahora.shared.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolDelSistemaRepository extends JpaRepository<RolDelSistema, Long> {

    Optional<RolDelSistema> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    /** El rol base, que es único por cada valor del enum. */
    Optional<RolDelSistema> findByRolBaseAndBaseTrue(Rol rolBase);

    List<RolDelSistema> findAllByOrderByBaseDescNombreAsc();
}
