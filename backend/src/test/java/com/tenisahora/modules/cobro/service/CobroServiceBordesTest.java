package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import com.tenisahora.modules.reserva.service.PagosDeReserva;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static com.tenisahora.modules.cobro.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CobroServiceBordesTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);
    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 7);

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
    private EmisionDeRecibos emisionDeRecibos;
    @Mock
    private ConfirmacionDeReserva confirmacionDeReserva;
    @Mock
    private ReemplazoDeQr reemplazoDeQr;
    @Mock
    private PagosDeReserva pagosDeReserva;
    @Mock
    private PdfService pdfService;

    private CobroServiceImpl servicio;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        servicio = new CobroServiceImpl(cobroRepository, reciboRepository, accesoAReservas, accesoACobros, cobroPorId,
                calculadoraDeCobro, formasDeCobro, emisionDeRecibos, confirmacionDeReserva, reemplazoDeQr, pagosDeReserva, new CobroMapper(), pdfService);
        when(cobroRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<Cobro>(List.of()));
        when(reciboRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<Recibo>(List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C-000123", "c-000123", "C123", "  C-1  ", "R-000123", "r-1", "R1", "C-000000000000000001"})
    void buscar_conFormatoValido_noFalla(String busqueda) {
        assertThatCode(() -> servicio.buscar(busqueda, null, null, null, null, 0, 20)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "C-", "R-", "C-12a", "X-000123", "C--1", "C 123", "C-1234567890123456789", "1; drop table cobro"})
    void buscar_conOtroFormato_400(String busqueda) {
        assertThatThrownBy(() -> servicio.buscar(busqueda, null, null, null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("CRITERIO_DE_BUSQUEDA_INVALIDO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void buscar_conTextoVacio_listaTodo(String busqueda) {
        assertThatCode(() -> servicio.buscar(busqueda, null, null, null, null, 0, 20)).doesNotThrowAnyException();
    }

    @Test
    void buscar_desdePosteriorAHasta_400() {
        assertThatThrownBy(() -> servicio.buscar(null, null, null, HOY.plusDays(1), HOY, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RANGO_DE_CONSULTA_INVALIDO");
    }

    @Test
    void buscar_rangoDeUnDiaOAbierto_noFalla() {
        assertThatCode(() -> {
            servicio.buscar(null, null, null, HOY, HOY, 0, 20);
            servicio.buscar(null, null, null, HOY, null, 0, 20);
            servicio.buscar(null, null, null, null, HOY, 0, 20);
        }).doesNotThrowAnyException();
    }

    @Test
    void buscarRecibos_desdePosteriorAHasta_400() {
        assertThatThrownBy(() -> servicio.buscarRecibos(null, HOY.plusDays(1), HOY, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RANGO_DE_CONSULTA_INVALIDO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"C-000123", "abc", "123"})
    void buscarRecibos_conOtroFormatoQueR_400(String busqueda) {
        assertThatThrownBy(() -> servicio.buscarRecibos(busqueda, null, null, 0, 20)).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void delSocio_sinCobros_devuelvePaginaVacia() {
        when(cobroRepository.delSocio(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThat(servicio.delSocio(3L, 0, 20).contenido()).isEmpty();
    }

    @Test
    void delSocio_devuelveCadaCobroConSuReciboYLosMasRecientesPrimero() {
        Cobro pagado = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        Cobro rechazado = cobro(8L, EstadoCobro.RECHAZADO, MedioPago.DEBITO, "8000.00");
        Recibo recibo = new Recibo();
        recibo.setCobro(pagado);
        recibo.setNumeroRecibo("R-000007");
        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        when(cobroRepository.delSocio(eq(3L), pedido.capture())).thenReturn(new PageImpl<>(List.of(rechazado, pagado)));
        when(reciboRepository.findByCobroIdIn(List.of(8L, 7L))).thenReturn(List.of(recibo));

        List<CobroResponse> cobros = servicio.delSocio(3L, 2, 10).contenido();

        assertThat(cobros).hasSize(2);
        assertThat(cobros.get(0).recibo()).isNull();
        assertThat(cobros.get(1).recibo().numeroRecibo()).isEqualTo("R-000007");
        assertThat(pedido.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pedido.getValue().getPageSize()).isEqualTo(10);
        assertThat(pedido.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void imprimir_unCobroSinReservaNiNumeroDeOperacion_noFalla() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setReserva(null);
        when(cobroPorId.buscar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        assertThatCode(() -> servicio.imprimir(7L, EMPLEADO)).doesNotThrowAnyException();

        verify(pdfService).generarFicha(any(), any());
    }

    @Test
    void imprimir_paraElSocioEsUnComprobanteDePago() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.EFECTIVO, "8000.00");
        when(cobroPorId.buscar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, SOCIO)).thenReturn(cobro);

        servicio.imprimir(7L, SOCIO);

        verify(pdfService).generarFicha(eq("Comprobante de pago"), any());
    }

    @Test
    void imprimir_paraElPersonalEsUnComprobanteDeCobro() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.EFECTIVO, "8000.00");
        when(cobroPorId.buscar(7L)).thenReturn(cobro);
        when(accesoACobros.visible(cobro, EMPLEADO)).thenReturn(cobro);

        servicio.imprimir(7L, EMPLEADO);

        verify(pdfService).generarFicha(eq("Comprobante de cobro"), any());
    }

    @ParameterizedTest
    @EnumSource(value = MedioPago.class, names = {"DEBITO", "CREDITO", "EFECTIVO"})
    void registrar_unSocioConUnMedioDelPersonal_403(MedioPago medio) {
        when(accesoAReservas.paraGestionar(10L, SOCIO, true)).thenReturn(reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00"));

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(medio, "1", null, true), SOCIO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = {"CANCELADA"})
    void registrar_unaReservaCancelada_422(EstadoReserva estado) {
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, estado, "16000.00"));

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.EFECTIVO, null, null, true), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_NO_COBRABLE");
    }

    @Test
    void registrar_conUnQrRechazado_cierraSuLinkYSiYaEstabaPago_noCobraDosVeces() {
        when(accesoAReservas.paraGestionar(10L, EMPLEADO, true)).thenReturn(reserva(10L, EstadoReserva.PENDIENTE_PAGO, "16000.00"));
        List<Cobro> rechazados = List.of(cobro(7L, EstadoCobro.RECHAZADO, MedioPago.QR, "8000.00"));
        when(cobroRepository.findByReservaIdAndEstado(10L, EstadoCobro.RECHAZADO)).thenReturn(rechazados);
        when(cobroRepository.existsByReservaIdAndEstadoIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(10L, new CobroRequest(MedioPago.EFECTIVO, null, null, true), EMPLEADO))
                .extracting("codigo").isEqualTo("COBRO_YA_REGISTRADO");

        verify(reemplazoDeQr).reemplazarRechazados(rechazados, EMPLEADO);
        assertThat(rechazados).hasSize(1);
    }
}
