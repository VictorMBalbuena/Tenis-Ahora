package com.tenisahora.modules.auth.job;

import com.tenisahora.modules.auth.service.BloqueoDeLogin;
import com.tenisahora.security.LimitadorDeIntentos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LimpiezaDeLimitesJobTest {

    @Mock
    private BloqueoDeLogin bloqueoDeLogin;
    @Mock
    private LimitadorDeIntentos limitadorDeIntentos;

    @InjectMocks
    private LimpiezaDeLimitesJob job;

    @Test
    void limpiar_purgaElBloqueoDeLoginYElLimitePorIp() {
        job.limpiar();

        verify(bloqueoDeLogin).purgar();
        verify(limitadorDeIntentos).purgar();
    }
}
