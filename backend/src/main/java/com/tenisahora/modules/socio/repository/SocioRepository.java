package com.tenisahora.modules.socio.repository;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.persona.RepositorioDePersonas;

import java.util.Optional;

public interface SocioRepository extends RepositorioDePersonas<Socio> {

    Optional<Socio> findByDniAndActivoTrue(String dni);
}
