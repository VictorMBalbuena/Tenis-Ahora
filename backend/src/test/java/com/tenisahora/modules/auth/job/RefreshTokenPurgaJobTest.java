package com.tenisahora.modules.auth.job;

import com.tenisahora.modules.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenPurgaJobTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenPurgaJob job;

    @Test
    void purgar_borraLosVencidosYRevocadosConLaHoraActual() {
        job.purgar();

        verify(refreshTokenRepository).purgarVencidosORevocados(any(LocalDateTime.class));
    }
}
