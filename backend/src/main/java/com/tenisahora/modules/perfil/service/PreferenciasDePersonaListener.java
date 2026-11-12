package com.tenisahora.modules.perfil.service;

import com.tenisahora.modules.perfil.repository.PreferenciasDeNotificacionRepository;
import com.tenisahora.shared.event.PersonaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Al eliminar una persona se borran sus preferencias, igual que se borran sus refresh tokens. */
@Component
@RequiredArgsConstructor
public class PreferenciasDePersonaListener {

    private final PreferenciasDeNotificacionRepository repositorio;

    @EventListener
    @Transactional
    public void alEliminarPersona(PersonaEliminadaEvent evento) {
        repositorio.deleteByPersonaId(evento.personaId());
    }
}
