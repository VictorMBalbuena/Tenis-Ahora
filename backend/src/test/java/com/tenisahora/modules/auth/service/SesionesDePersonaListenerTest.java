package com.tenisahora.modules.auth.service;

import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import com.tenisahora.shared.event.PersonaEliminadaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SesionesDePersonaListenerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private SesionesDePersonaListener listener;

    @Test
    void alEliminarPersona_borraSusRefreshTokens() {
        listener.alEliminarPersona(new PersonaEliminadaEvent(7L));

        verify(refreshTokenRepository).borrarTodosDeLaPersona(7L);
    }
}
