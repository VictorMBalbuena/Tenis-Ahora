package com.tenisahora.modules.reserva.job;

import com.tenisahora.modules.reserva.service.CancelacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiracionDePendientesJobTest {

    @Mock
    private CancelacionService cancelacionService;
    @InjectMocks
    private ExpiracionDePendientesJob job;

    @Test
    void vencerPendientes_lePideAlServicioQueCanceleLasVencidas() {
        when(cancelacionService.cancelarVencidas()).thenReturn(3);

        job.vencerPendientes();

        verify(cancelacionService).cancelarVencidas();
    }

    @Test
    void vencerPendientes_sinNadaParaCancelar_noFalla() {
        when(cancelacionService.cancelarVencidas()).thenReturn(0);

        job.vencerPendientes();

        verify(cancelacionService).cancelarVencidas();
    }
}
