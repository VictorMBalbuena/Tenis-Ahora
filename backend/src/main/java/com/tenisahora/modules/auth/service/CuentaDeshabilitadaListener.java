package com.tenisahora.modules.auth.service;

import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import com.tenisahora.shared.event.CuentaDeshabilitadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al deshabilitar una cuenta se le cierran las sesiones abiertas, como pide RF76.
 *
 * <p>Con esto alcanza: el access token que la persona tenga en la mano deja de servir en el pedido siguiente,
 * porque el filtro JWT vuelve a mirar la base y no deja pasar a una cuenta inactiva. Borrar los refresh tokens
 * evita además que se renueve la sesión.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CuentaDeshabilitadaListener {

    private final RefreshTokenRepository refreshTokenRepository;

    @EventListener
    public void alDeshabilitar(CuentaDeshabilitadaEvent evento) {
        refreshTokenRepository.borrarTodosDeLaPersona(evento.personaId());
        log.info("Sesiones cerradas por deshabilitación de la persona {}", evento.personaId());
    }
}
