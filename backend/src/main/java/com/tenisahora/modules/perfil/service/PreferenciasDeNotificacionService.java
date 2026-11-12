package com.tenisahora.modules.perfil.service;

import com.tenisahora.modules.perfil.dto.PreferenciasDeNotificacion;
import com.tenisahora.modules.perfil.entity.PreferenciasDeNotificacionEntity;
import com.tenisahora.modules.perfil.repository.PreferenciasDeNotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** GET y PUT /me/preferencias. Si la persona nunca las guardó, se completa con los valores por defecto. */
@Service
@RequiredArgsConstructor
public class PreferenciasDeNotificacionService {

    private final PreferenciasDeNotificacionRepository repositorio;

    @Transactional(readOnly = true)
    public PreferenciasDeNotificacion obtener(Long personaId) {
        return repositorio.findById(personaId).map(this::aDto).orElseGet(PreferenciasDeNotificacion::porDefecto);
    }

    @Transactional
    public PreferenciasDeNotificacion guardar(Long personaId, PreferenciasDeNotificacion cambios) {
        PreferenciasDeNotificacionEntity entidad = repositorio.findById(personaId)
                .orElseGet(() -> new PreferenciasDeNotificacionEntity(personaId,
                        PreferenciasDeNotificacion.RECORDATORIO_RESERVAS_POR_DEFECTO,
                        PreferenciasDeNotificacion.RECORDATORIO_CLASES_POR_DEFECTO,
                        PreferenciasDeNotificacion.NOVEDADES_DEL_CLUB_POR_DEFECTO));

        entidad.setRecordatorioReservas(cambios.recordatorioReservas());
        entidad.setRecordatorioClases(cambios.recordatorioClases());
        entidad.setNovedadesDelClub(cambios.novedadesDelClub());

        return aDto(repositorio.save(entidad));
    }

    private PreferenciasDeNotificacion aDto(PreferenciasDeNotificacionEntity entidad) {
        return new PreferenciasDeNotificacion(entidad.isRecordatorioReservas(), entidad.isRecordatorioClases(),
                entidad.isNovedadesDelClub());
    }
}
