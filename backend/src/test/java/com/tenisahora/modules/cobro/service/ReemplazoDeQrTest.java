package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReemplazoDeQrTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);

    @Mock
    private AcreditacionDePagos acreditacionDePagos;
    @Mock
    private CierreDeOrden cierreDeOrden;
    @Mock
    private RegistroDeAnulacion registroDeAnulacion;
    @InjectMocks
    private ReemplazoDeQr reemplazo;

    private Cobro cobro(EstadoCobro estado, MedioPago medio) {
        Cobro cobro = new Cobro();
        cobro.setEstado(estado);
        cobro.setMedioPago(medio);
        cobro.setIdPagoExterno("orden-1");
        return cobro;
    }

    @Test
    void reemplazarRechazados_anulaElCobroYCierraElLink() {
        Cobro rechazado = cobro(EstadoCobro.RECHAZADO, MedioPago.QR);

        reemplazo.reemplazarRechazados(List.of(rechazado), EMPLEADO);

        verify(registroDeAnulacion).aplicar(any(), any(), any());
        verify(cierreDeOrden).cerrar("orden-1");
    }

    @Test
    void reemplazarRechazados_siYaLoPagaron_noLoAnula() {
        Cobro rechazado = cobro(EstadoCobro.RECHAZADO, MedioPago.QR);
        doAnswer(inv -> {
            rechazado.setEstado(EstadoCobro.PAGADO);
            return null;
        }).when(acreditacionDePagos).acreditar("orden-1");

        reemplazo.reemplazarRechazados(List.of(rechazado), EMPLEADO);

        verify(registroDeAnulacion, never()).aplicar(any(), any(), any());
        verify(cierreDeOrden, never()).cerrar(any());
    }

    @Test
    void reemplazarRechazados_unaTarjetaRechazada_noTieneLinkQueCerrar() {
        reemplazo.reemplazarRechazados(List.of(cobro(EstadoCobro.RECHAZADO, MedioPago.DEBITO)), EMPLEADO);

        verifyNoInteractions(acreditacionDePagos, cierreDeOrden, registroDeAnulacion);
    }
}
