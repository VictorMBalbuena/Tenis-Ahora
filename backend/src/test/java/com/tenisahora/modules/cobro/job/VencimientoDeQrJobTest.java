package com.tenisahora.modules.cobro.job;

import com.tenisahora.modules.cobro.service.VencimientoDeQr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VencimientoDeQrJobTest {

    @Mock
    private VencimientoDeQr vencimientoDeQr;
    @InjectMocks
    private VencimientoDeQrJob job;

    @Test
    void vencerExpirados_lePideAlServicioQueVenzaLosQr() {
        when(vencimientoDeQr.vencerExpirados()).thenReturn(2);

        job.vencerExpirados();

        verify(vencimientoDeQr).vencerExpirados();
    }

    @Test
    void vencerExpirados_sinNadaParaVencer_noFalla() {
        when(vencimientoDeQr.vencerExpirados()).thenReturn(0);

        job.vencerExpirados();

        verify(vencimientoDeQr).vencerExpirados();
    }
}
