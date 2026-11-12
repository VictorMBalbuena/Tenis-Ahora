package com.tenisahora.shared.persona;

import com.tenisahora.shared.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/** Lo que tiene en común el repositorio de cada rol. Cada rol declara el suyo sobre su propia entidad. */
@NoRepositoryBean
public interface RepositorioDePersonas<E extends Persona> extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {

    Optional<E> findByEmail(String email);

    boolean existsByDni(String dni);

    boolean existsByEmailIgnoreCase(String email);
}
