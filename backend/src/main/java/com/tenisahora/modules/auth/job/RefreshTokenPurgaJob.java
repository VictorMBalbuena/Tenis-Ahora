package com.tenisahora.modules.auth.job;

import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Borra los refresh tokens vencidos o revocados para que la tabla no crezca sin límite. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenPurgaJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgar() {
        int borrados = refreshTokenRepository.purgarVencidosORevocados(LocalDateTime.now());
        log.info("Purga de refresh tokens: {} borrados", borrados);
    }
}
