package com.tenisahora.modules.auth.service;

import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import com.tenisahora.shared.event.PersonaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al eliminar un usuario se borran sus refresh tokens (en la misma transacción de la baja). */
@Component
@RequiredArgsConstructor
public class SesionesDePersonaListener {

    private final RefreshTokenRepository refreshTokenRepository;

    @EventListener
    public void alEliminarPersona(PersonaEliminadaEvent evento) {
        refreshTokenRepository.borrarTodosDeLaPersona(evento.personaId());
    }
}
