package com.tenisahora.modules.reserva.job;

import com.tenisahora.modules.reserva.service.CicloDelAlquiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalizacionAutomaticaJobTest {

    @Mock
    private CicloDelAlquiler cicloDelAlquiler;
    @InjectMocks
    private FinalizacionAutomaticaJob job;

    @Test
    void finalizarVencidas_lePideAlServicioQueCierreLasReservasEnCursoVencidas() {
        when(cicloDelAlquiler.finalizarVencidas()).thenReturn(2);

        job.finalizarVencidas();

        verify(cicloDelAlquiler).finalizarVencidas();
    }

    @Test
    void finalizarVencidas_sinNadaParaCerrar_noFalla() {
        when(cicloDelAlquiler.finalizarVencidas()).thenReturn(0);

        job.finalizarVencidas();

        verify(cicloDelAlquiler).finalizarVencidas();
    }
}
