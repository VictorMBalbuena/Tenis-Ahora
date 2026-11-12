package com.tenisahora.modules.perfil.repository;

import com.tenisahora.modules.perfil.entity.PreferenciasDeNotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenciasDeNotificacionRepository extends JpaRepository<PreferenciasDeNotificacionEntity, Long> {

    void deleteByPersonaId(Long personaId);
}
