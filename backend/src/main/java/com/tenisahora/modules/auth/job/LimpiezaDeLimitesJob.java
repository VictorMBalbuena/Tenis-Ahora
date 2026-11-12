package com.tenisahora.modules.auth.job;

import com.tenisahora.modules.auth.service.BloqueoDeLogin;
import com.tenisahora.security.LimitadorDeIntentos;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpia de la memoria los contadores vencidos del bloqueo de login y del límite por IP. */
@Component
@RequiredArgsConstructor
public class LimpiezaDeLimitesJob {

    private final BloqueoDeLogin bloqueoDeLogin;
    private final LimitadorDeIntentos limitadorDeIntentos;

    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void limpiar() {
        bloqueoDeLogin.purgar();
        limitadorDeIntentos.purgar();
    }
}
