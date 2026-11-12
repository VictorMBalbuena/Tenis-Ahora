package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.dto.QrResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.cobro.service.pago.MedioDePago;
import com.tenisahora.modules.cobro.service.pago.ResultadoDeCobro;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrDeCobroImplTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);

    @Mock
    private CobroPorId cobroPorId;
    @Mock
    private AccesoACobros accesoACobros;
    @Mock
    private FormasDeCobro formasDeCobro;
    @Mock
    private MedioDePago medioDePago;
    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private AcreditacionDePagos acreditacionDePagos;
    @Mock
    private CierreDeOrden cierreDeOrden;

    private QrDeCobroImpl qrDeCobro;

    @BeforeEach
    void setUp() {
        qrDeCobro = new QrDeCobroImpl(cobroPorId, accesoACobros, formasDeCobro, cobroRepository, new CobroMapper(), acreditacionDePagos, cierreDeOrden);
    }

    @Test
    void generar_unCobroPendienteDeQr_pideUnaOrdenNuevaYLaDevuelve() {
        Cobro cobro = cobro(7L, EstadoCobro.VENCIDO, MedioPago.QR, "8000.00");
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);
        when(formasDeCobro.de(MedioPago.QR)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.pendienteConQr("orden-2", "data-nueva", null));

        QrResponse respuesta = qrDeCobro.generar(7L, EMPLEADO);

        assertThat(respuesta.qrData()).isEqualTo("data-nueva");
        assertThat(respuesta.estado()).isEqualTo(EstadoCobro.PENDIENTE);
        assertThat(cobro.getIdPagoExterno()).isEqualTo("orden-2");
    }

    @Test
    void generar_conUnLinkAnterior_loCierraAntesDePedirOtro() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);
        when(formasDeCobro.de(MedioPago.QR)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.pendienteConQr("orden-2", "data-nueva", null));

        qrDeCobro.generar(7L, EMPLEADO);

        org.mockito.Mockito.verify(cierreDeOrden).cerrar("orden-1");
    }

    @Test
    void verificar_unQrPendiente_leConsultaALaPasarela() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroPorId.buscar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        qrDeCobro.verificar(7L, EMPLEADO);

        org.mockito.Mockito.verify(acreditacionDePagos).acreditar("orden-1");
    }

    @Test
    void verificar_unCobroYaPagado_noVuelveAConsultar() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroPorId.buscar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        qrDeCobro.verificar(7L, EMPLEADO);

        org.mockito.Mockito.verifyNoInteractions(acreditacionDePagos);
    }

    @Test
    void generar_conLaReservaCancelada_422() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.getReserva().setEstado(com.tenisahora.modules.reserva.entity.EstadoReserva.CANCELADA);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        assertThatThrownBy(() -> qrDeCobro.generar(7L, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void generar_cobroQueNoEsDeQr_422() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.DEBITO, "8000.00");
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        assertThatThrownBy(() -> qrDeCobro.generar(7L, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("COBRO_NO_ADMITE_QR");
    }

    @Test
    void generar_cobroYaPagado_422() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        assertThatThrownBy(() -> qrDeCobro.generar(7L, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
