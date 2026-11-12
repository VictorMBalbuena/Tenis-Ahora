package com.tenisahora.modules.cobro.job;

import com.tenisahora.modules.cobro.service.ConciliacionDePagos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciliacionDePagosJobTest {

    @Mock
    private ConciliacionDePagos conciliacionDePagos;
    @InjectMocks
    private ConciliacionDePagosJob job;

    @Test
    void conciliar_lePideAlServicioQueConcilie() {
        when(conciliacionDePagos.conciliar()).thenReturn(1);

        job.conciliar();

        verify(conciliacionDePagos).conciliar();
    }

    @Test
    void conciliar_sinNadaParaConciliar_noFalla() {
        when(conciliacionDePagos.conciliar()).thenReturn(0);

        job.conciliar();

        verify(conciliacionDePagos).conciliar();
    }
}
