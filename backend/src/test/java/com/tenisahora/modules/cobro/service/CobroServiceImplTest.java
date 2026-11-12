package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.modules.cobro.service.pago.DatosDeCobro;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.cobro.service.pago.MedioDePago;
import com.tenisahora.modules.cobro.service.pago.ResultadoDeCobro;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

import static com.tenisahora.modules.cobro.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CobroServiceImplTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);
    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);

    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private ReciboRepository reciboRepository;
    @Mock
    private AccesoAReservas accesoAReservas;
    @Mock
    private AccesoACobros accesoACobros;
    @Mock
    private CobroPorId cobroPorId;
    @Mock
    private CalculadoraDeCobro calculadoraDeCobro;
    @Mock
    private FormasDeCobro formasDeCobro;
    @Mock
    private MedioDePago medioDePago;
    @Mock
    private EmisionDeRecibos emisionDeRecibos;
    @Mock
    private ConfirmacionDeReserva confirmacionDeReserva;
    @Mock
    private ReemplazoDeQr reemplazoDeQr;
    @Mock
    private com.tenisahora.modules.reserva.service.PagosDeReserva pagosDeReserva;
    @Mock
    private PdfService pdfService;

    private CobroServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CobroServiceImpl(cobroRepository, reciboRepository, accesoAReservas, accesoACobros, cobroPorId,
                calculadoraDeCobro, formasDeCobro, emisionDeRecibos, confirmacionDeReserva, reemplazoDeQr, pagosDeReserva, new CobroMapper(), pdfService);
    }

    /** Simula que guardar asigna el id (como hace JPA con IDENTITY) y deja listos los montos para el registro. */
    private void conMontos() {
        when(cobroRepository.saveAndFlush(any(Cobro.class))).thenAnswer(inv -> {
            Cobro cobro = inv.getArgument(0);
            if (cobro.getId() == null) {
                cobro.setId(7L);
            }
            return cobro;
        });
        when(calculadoraDeCobro.montoBase(any())).thenReturn(new BigDecimal("8000.00"));
        when(calculadoraDeCobro.aplicarDescuentos(eq(7L), any(), any()))
                .thenReturn(new CalculadoraDeCobro.MontoDeCobro(new BigDecimal("8000.00"), new BigDecimal("8000.00"), List.of()));
    }

    @Test
    void registrar_debitoAprobado_quedaPagadoEmiteReciboYConfirmaLaReserva() {
        Reserva reservaPendiente = reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00");
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reservaPendiente);
        when(formasDeCobro.de(MedioPago.DEBITO)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.pagado("000123"));
        conMontos();

        CobroResponse respuesta = servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "000123", null, true), EMPLEADO);

        assertThat(respuesta.estado()).isEqualTo(EstadoCobro.PAGADO);
        assertThat(respuesta.codigo()).isEqualTo("C-000007");
        verify(emisionDeRecibos).emitir(any());
        verify(confirmacionDeReserva).confirmar(10L);
    }

    @Test
    void registrar_tarjetaRechazada_A1_quedaRechazadoYNoConfirmaLaReserva() {
        Reserva reservaPendiente = reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00");
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reservaPendiente);
        when(formasDeCobro.de(MedioPago.CREDITO)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.rechazado("000999"));
        conMontos();

        CobroResponse respuesta = servicio.registrar(10L, new CobroRequest(MedioPago.CREDITO, "000999", 3, false), EMPLEADO);

        assertThat(respuesta.estado()).isEqualTo(EstadoCobro.RECHAZADO);
        verify(emisionDeRecibos, never()).emitir(any());
        verify(confirmacionDeReserva, never()).confirmar(any());
    }

    @Test
    void registrar_conQr_quedaPendienteConElQrData() {
        Reserva reservaPendiente = reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00");
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reservaPendiente);
        when(formasDeCobro.de(MedioPago.QR)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.pendienteConQr("orden-1", "data-del-qr", null));
        conMontos();

        CobroResponse respuesta = servicio.registrar(10L, new CobroRequest(MedioPago.QR, null, null, true), EMPLEADO);

        assertThat(respuesta.estado()).isEqualTo(EstadoCobro.PENDIENTE);
        assertThat(respuesta.qrData()).isEqualTo("data-del-qr");
        verify(confirmacionDeReserva, never()).confirmar(any());
    }

    @Test
    void registrar_qrConUnQrYaEnCurso_devuelveElMismoCobro() {
        com.tenisahora.modules.cobro.entity.Cobro enCurso = com.tenisahora.modules.cobro.DatosDePrueba.cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(accesoAReservas.paraGestionar(10L, SOCIO, true)).thenReturn(reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00"));
        when(cobroRepository.findFirstByReservaIdAndEstadoAndMedioPago(10L, EstadoCobro.PENDIENTE, MedioPago.QR)).thenReturn(java.util.Optional.of(enCurso));

        CobroResponse respuesta = servicio.registrar(10L, new CobroRequest(MedioPago.QR, null, null, true), SOCIO);

        assertThat(respuesta.id()).isEqualTo(7L);
        verify(cobroRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_otroMedioConUnQrEnCurso_reemplazaElQr() {
        com.tenisahora.modules.cobro.entity.Cobro enCurso = com.tenisahora.modules.cobro.DatosDePrueba.cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00"));
        when(cobroRepository.findFirstByReservaIdAndEstadoAndMedioPago(10L, EstadoCobro.PENDIENTE, MedioPago.QR)).thenReturn(java.util.Optional.of(enCurso));
        when(cobroRepository.existsByReservaIdAndEstadoIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "1", null, true), EMPLEADO))
                .isInstanceOf(ConflictoException.class);

        verify(reemplazoDeQr).reemplazar(enCurso, EMPLEADO);
    }

    @Test
    void registrar_conLaSeniaPaga_cobraElSaldoYNoVuelveAConfirmar() {
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, EstadoReserva.CONFIRMADA, "16000.00"));
        when(pagosDeReserva.resumen(10L)).thenReturn(new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(new BigDecimal("8000.00"), new BigDecimal("8000.00")));
        when(formasDeCobro.de(MedioPago.DEBITO)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.pagado("000123"));
        when(cobroRepository.saveAndFlush(any(com.tenisahora.modules.cobro.entity.Cobro.class))).thenAnswer(inv -> {
            com.tenisahora.modules.cobro.entity.Cobro cobro = inv.getArgument(0);
            cobro.setId(7L);
            return cobro;
        });
        when(calculadoraDeCobro.aplicarDescuentos(eq(7L), any(), any()))
                .thenReturn(new CalculadoraDeCobro.MontoDeCobro(new BigDecimal("8000.00"), new BigDecimal("8000.00"), List.of()));

        CobroResponse respuesta = servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "000123", null, true), EMPLEADO);

        assertThat(respuesta.concepto()).isEqualTo(com.tenisahora.modules.cobro.entity.ConceptoCobro.SALDO);
        assertThat(respuesta.montoFinal()).isEqualByComparingTo("8000.00");
        verify(confirmacionDeReserva, never()).confirmar(any());
    }

    @Test
    void registrar_sinSaldoPendiente_409() {
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, EstadoReserva.CONFIRMADA, "16000.00"));
        when(pagosDeReserva.resumen(10L)).thenReturn(new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(new BigDecimal("16000.00"), new BigDecimal("16000.00")));

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "1", null, true), EMPLEADO))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void registrar_socioConDebitoOCredito_403_noPuedeAutoaprobarseElPago() {
        when(accesoAReservas.paraGestionar(10L, SOCIO, true)).thenReturn(reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00"));

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "000123", null, true), SOCIO))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.CREDITO, "000123", 1, true), SOCIO))
                .isInstanceOf(AccessDeniedException.class);
        verify(cobroRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_socioConQr_permitido() {
        Reserva reservaPendiente = reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00");
        when(accesoAReservas.paraGestionar(10L, SOCIO, true)).thenReturn(reservaPendiente);
        when(formasDeCobro.de(MedioPago.QR)).thenReturn(medioDePago);
        when(medioDePago.cobrar(any(), any())).thenReturn(ResultadoDeCobro.pendienteConQr("orden-1", "data-del-qr", null));
        conMontos();

        CobroResponse respuesta = servicio.registrar(10L, new CobroRequest(MedioPago.QR, null, null, true), SOCIO);

        assertThat(respuesta.estado()).isEqualTo(EstadoCobro.PENDIENTE);
    }

    @Test
    void registrar_reservaNoCobrable_422() {
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, EstadoReserva.CANCELADA, "16000.00"));

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "1", null, true), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_NO_COBRABLE");
    }

    @Test
    void registrar_yaTieneUnCobroRegistrado_409() {
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00"));
        when(cobroRepository.existsByReservaIdAndEstadoIn(eq(10L), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.DEBITO, "1", null, true), EMPLEADO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("COBRO_YA_REGISTRADO");
    }

    @Test
    void consultar_delegaEnElAccesoYElMapper() {
        Cobro cobro = com.tenisahora.modules.cobro.DatosDePrueba.cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        when(cobroPorId.buscar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        CobroResponse respuesta = servicio.consultar(7L, EMPLEADO);

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.qrData()).isNull();
    }
}
