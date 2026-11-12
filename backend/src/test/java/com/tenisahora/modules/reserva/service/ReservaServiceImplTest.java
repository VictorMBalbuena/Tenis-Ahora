package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.InsumoPedidoRequest;
import com.tenisahora.modules.reserva.dto.JugadorRequest;
import com.tenisahora.modules.reserva.dto.ReservaRequest;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.dto.ReservaUpdateRequest;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.modules.reserva.mapper.ReservaMapper;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaJugadorRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.reserva.DatosDePrueba.cancha;
import static com.tenisahora.modules.reserva.DatosDePrueba.persona;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final LocalDate FECHA = HOY.plusDays(5);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaJugadorRepository jugadorRepository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private ValidadorDeAgenda validadorDeAgenda;
    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private AsignacionDeInsumos asignacionDeInsumos;
    @Mock
    private DescuentoPorCodigo descuentoPorCodigo;

    private ReservaServiceImpl servicio;
    private Cancha cancha1;

    @BeforeEach
    void setUp() {
        CalculadoraDeMontos calculadora = new CalculadoraDeMontos(PROPIEDADES);
        ReservaMapper mapper = new ReservaMapper(calculadora, PROPIEDADES, id -> new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));
        servicio = new ReservaServiceImpl(reservaRepository, jugadorRepository, canchaRepository, socioRepository,
                new ReglasDeReserva(PROPIEDADES, RELOJ), calculadora, validadorDeAgenda, new ArmadoDeJugadores(socioRepository),
                new ArmadoDeRespuestas(jugadorRepository, reservaInsumoRepository, mapper), mapper,
                new AccesoAReservas(reservaRepository, jugadorRepository), asignacionDeInsumos, descuentoPorCodigo, RELOJ);
        cancha1 = cancha(1L, "Cancha 1", "8000");
    }

    private static ReservaRequest pedido(Long titularId, JugadorRequest... acompaniantes) {
        return new ReservaRequest(1L, FECHA, LocalTime.of(18, 0), 2, titularId, List.of(acompaniantes), null);
    }

    private static JugadorRequest invitado() {
        return new JugadorRequest(null, "Luciana", "Fernández", "30123456");
    }

    private void cancha1Existe() {
        when(canchaRepository.buscarParaReservar(1L)).thenReturn(Optional.of(cancha1));
    }

    private void guardadoAsignaId() {
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> {
            Reserva guardada = inv.getArgument(0);
            guardada.setId(10L);
            return guardada;
        });
    }

    // ---------- CU16 ----------

    @Test
    @SuppressWarnings("unchecked")
    void registrar_comoSocio_creaLaReservaPendienteConMontosCalculadosPorElServidor() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();

        ReservaResponse respuesta = servicio.registrar(pedido(null, invitado()), SOCIO);

        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.numero()).isEqualTo("R-000010");
        assertThat(respuesta.estado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(respuesta.horaInicio()).isEqualTo(LocalTime.of(18, 0));
        assertThat(respuesta.horaFin()).isEqualTo(LocalTime.of(20, 0));
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("16000.00");
        assertThat(respuesta.montoSenia()).isEqualByComparingTo("8000.00");
        assertThat(respuesta.venceEn()).isEqualTo(com.tenisahora.modules.reserva.DatosDePrueba.AHORA.plusMinutes(30));
        assertThat(respuesta.cantidadJugadores()).isEqualTo(2);
        assertThat(respuesta.jugadores()).extracting(j -> j.titular()).containsExactly(true, false);
        verify(validadorDeAgenda).asegurarTurnoLibre(cancha1, FECHA, new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(20, 0)), null);
        ArgumentCaptor<List<ReservaJugador>> guardados = ArgumentCaptor.forClass(List.class);
        verify(jugadorRepository).saveAll(guardados.capture());
        assertThat(guardados.getValue()).hasSize(2);
    }

    @Test
    void registrar_comoEmpleado_conUnSocioActivoComoTitular() {
        when(socioRepository.findById(5L)).thenReturn(Optional.of(persona(5L, true)));
        cancha1Existe();
        guardadoAsignaId();

        ReservaResponse respuesta = servicio.registrar(pedido(5L, invitado()), EMPLEADO);

        assertThat(respuesta.jugadores().get(0).personaId()).isEqualTo(5L);
    }

    @Test
    void registrar_comoEmpleadoSinIndicarElTitular_400() {
        assertThatThrownBy(() -> servicio.registrar(pedido(null, invitado()), EMPLEADO))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RESERVA_TITULAR_INVALIDO");
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conUnTitularQueNoEsSocioActivo_422() {
        when(socioRepository.findById(5L)).thenReturn(Optional.empty()); // una cuenta de otro rol no está entre los socios
        when(socioRepository.findById(6L)).thenReturn(Optional.of(persona(6L, false)));
        when(socioRepository.findById(7L)).thenReturn(Optional.empty());

        for (long titular : new long[]{5L, 6L, 7L}) {
            assertThatThrownBy(() -> servicio.registrar(pedido(titular, invitado()), EMPLEADO))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_TITULAR_INVALIDO");
        }
    }

    @Test
    void registrar_unSocioQuiereReservarAnombreDeOtro_403() {
        assertThatThrownBy(() -> servicio.registrar(pedido(99L, invitado()), SOCIO))
                .isInstanceOf(AccessDeniedException.class);
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conTresJugadores_400_A3_yNoGuardaNada() {
        assertThatThrownBy(() -> servicio.registrar(pedido(null, invitado(), invitado()), SOCIO))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RESERVA_JUGADORES_INVALIDOS");
        verify(canchaRepository, never()).buscarParaReservar(any());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conCuatroJugadores_sePermite() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();

        ReservaResponse respuesta = servicio.registrar(pedido(null, invitado(), invitado(), invitado()), SOCIO);

        assertThat(respuesta.cantidadJugadores()).isEqualTo(4);
    }

    @Test
    void registrar_aMasDe30Dias_422_yNoPideLaCancha() {
        ReservaRequest lejano = new ReservaRequest(1L, HOY.plusDays(31), LocalTime.of(18, 0), 1, null, List.of(invitado()), null);

        assertThatThrownBy(() -> servicio.registrar(lejano, SOCIO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_FUERA_DE_PLAZO");
        verify(canchaRepository, never()).buscarParaReservar(any());
    }

    @Test
    void registrar_conMasDeDosHoras_400() {
        ReservaRequest larga = new ReservaRequest(1L, FECHA, LocalTime.of(18, 0), 3, null, List.of(invitado()), null);

        assertThatThrownBy(() -> servicio.registrar(larga, SOCIO))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RESERVA_DURACION_INVALIDA");
    }

    @Test
    void registrar_enUnaCanchaInexistente_404() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        when(canchaRepository.buscarParaReservar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(pedido(null, invitado()), SOCIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("CANCHA_NO_ENCONTRADA");
    }

    @Test
    void registrar_enUnHorarioOcupadoOBloqueado_noGuardaNada() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.RESERVA_HORARIO_OCUPADO, "Ese horario ya está reservado"))
                .when(validadorDeAgenda).asegurarTurnoLibre(any(), any(), any(), any());

        assertThatThrownBy(() -> servicio.registrar(pedido(null, invitado()), SOCIO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("RESERVA_HORARIO_OCUPADO");
        verify(reservaRepository, never()).saveAndFlush(any());
        verify(jugadorRepository, never()).saveAll(anyList());
    }

    @Test
    void registrar_conUnInvitadoSinApellido_400_antesDeGuardar() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();

        assertThatThrownBy(() -> servicio.registrar(pedido(null, new JugadorRequest(null, "Luciana", null, null)), SOCIO))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    // ---------- Consultas ----------

    @Test
    void consultar_unSocioVeUnaReservaDondeParticipa() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva(10L, EstadoReserva.CONFIRMADA, FECHA, LocalTime.of(18, 0))));
        when(jugadorRepository.existsByReservaIdAndSocioId(10L, 3L)).thenReturn(true);

        assertThat(servicio.consultar(10L, SOCIO).numero()).isEqualTo("R-000010");
    }

    @Test
    void consultar_unSocioNoVeUnaReservaAjena_404() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva(10L, EstadoReserva.CONFIRMADA, FECHA, LocalTime.of(18, 0))));
        when(jugadorRepository.existsByReservaIdAndSocioId(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.consultar(10L, SOCIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("RESERVA_NO_ENCONTRADA");
    }

    @Test
    void consultar_elPersonalVeCualquierReserva() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva(10L, EstadoReserva.CONFIRMADA, FECHA, LocalTime.of(18, 0))));

        assertThat(servicio.consultar(10L, EMPLEADO).id()).isEqualTo(10L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void misReservas_devuelveLaPaginaDelSocio() {
        when(reservaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reserva(10L, EstadoReserva.CONFIRMADA, FECHA, LocalTime.of(18, 0))), PageRequest.of(0, 20), 1));

        PageResponse<ReservaResponse> pagina = servicio.misReservas(true, EstadoReserva.CONFIRMADA, 0, 20, SOCIO);

        assertThat(pagina.contenido()).extracting(ReservaResponse::id).containsExactly(10L);
        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_conNumeroEnCualquierFormato_noLanza() {
        when(reservaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        for (String numero : new String[]{"R-000123", "r-123", "123", " 000123 "}) {
            assertThat(servicio.buscar(null, numero, null, null, null, 0, 20).contenido()).isEmpty();
        }
    }

    @Test
    void buscar_conUnNumeroMalEscrito_400() {
        assertThatThrownBy(() -> servicio.buscar(null, "abc", null, null, null, 0, 20))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("CRITERIO_DE_BUSQUEDA_INVALIDO");
    }

    // ---------- CU17 ----------

    private Reserva reservaExistente() {
        Reserva existente = reserva(10L, EstadoReserva.CONFIRMADA, FECHA, LocalTime.of(18, 0));
        existente.setCancha(cancha1);
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(existente));
        return existente;
    }

    @Test
    void modificar_conMenosHoras_recalculaElMontoYVerificaElNuevoTurnoSinContarseASiMisma() {
        Reserva existente = reservaExistente();
        cancha1Existe();

        ReservaResponse respuesta = servicio.modificar(10L, new ReservaUpdateRequest(null, null, null, 1), EMPLEADO);

        assertThat(respuesta.horaFin()).isEqualTo(LocalTime.of(19, 0));
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("8000.00");
        assertThat(existente.getHoraFin()).isEqualTo(LocalTime.of(19, 0));
        verify(validadorDeAgenda).asegurarTurnoLibre(cancha1, FECHA, new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(19, 0)), 10L);
        verify(reservaRepository).saveAndFlush(existente);
    }

    @Test
    void modificar_aOtraCanchaYOtroHorario_tomaElPrecioDeLaNuevaCancha() {
        reservaExistente();
        Cancha cancha2 = cancha(2L, "Cancha 2", "6000");
        when(canchaRepository.buscarParaReservar(2L)).thenReturn(Optional.of(cancha2));

        ReservaResponse respuesta = servicio.modificar(10L, new ReservaUpdateRequest(2L, FECHA.plusDays(1), LocalTime.of(10, 0), null), EMPLEADO);

        assertThat(respuesta.canchaId()).isEqualTo(2L);
        assertThat(respuesta.fecha()).isEqualTo(FECHA.plusDays(1));
        assertThat(respuesta.horaInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(respuesta.horaFin()).isEqualTo(LocalTime.of(12, 0));
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("12000.00");
    }

    @Test
    void modificar_aUnHorarioOcupado_409_A1() {
        reservaExistente();
        cancha1Existe();
        doThrow(new ConflictoException(com.tenisahora.shared.exception.CodigoError.RESERVA_HORARIO_BLOQUEADO, "mantenimiento"))
                .when(validadorDeAgenda).asegurarTurnoLibre(any(), any(), any(), any());

        assertThatThrownBy(() -> servicio.modificar(10L, new ReservaUpdateRequest(null, null, LocalTime.of(19, 0), null), EMPLEADO))
                .isInstanceOf(ConflictoException.class);
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_unaReservaCanceladaOFinalizada_422() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.CANCELADA, EstadoReserva.FINALIZADA}) {
            when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva(10L, estado, FECHA, LocalTime.of(18, 0))));

            assertThatThrownBy(() -> servicio.modificar(10L, new ReservaUpdateRequest(null, null, null, 1), EMPLEADO))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_NO_MODIFICABLE");
        }
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_aMasDe30Dias_422() {
        reservaExistente();

        assertThatThrownBy(() -> servicio.modificar(10L, new ReservaUpdateRequest(null, HOY.plusDays(40), null, null), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_FUERA_DE_PLAZO");
    }

    @Test
    void modificar_inexistente_404() {
        when(reservaRepository.buscarParaModificar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.modificar(99L, new ReservaUpdateRequest(null, null, null, 1), EMPLEADO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- Insumos (CU13 dentro de CU16 y CU17) ----------

    @Test
    void registrar_conInsumos_losAsignaEnLaMismaOperacion() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();
        List<InsumoPedidoRequest> insumos = List.of(new InsumoPedidoRequest(1L, 4));
        ReservaRequest conInsumos = new ReservaRequest(1L, FECHA, LocalTime.of(18, 0), 2, null, List.of(invitado()), insumos);

        servicio.registrar(conInsumos, SOCIO);

        verify(asignacionDeInsumos).asignar(any(Reserva.class), org.mockito.ArgumentMatchers.eq(insumos));
    }

    @Test
    void registrar_sinInsumos_pasaUnPedidoNulo_yNoFalla() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();

        servicio.registrar(pedido(null, invitado()), SOCIO);

        verify(asignacionDeInsumos).asignar(any(Reserva.class), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void registrar_siNoHayStockDeInsumos_lanzaElErrorYLaReservaNoQueda() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();
        when(asignacionDeInsumos.asignar(any(Reserva.class), any()))
                .thenThrow(new ReglaDeNegocioException(com.tenisahora.shared.exception.CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay 0 unidades de Raquetas para ese horario"));

        assertThatThrownBy(() -> servicio.registrar(new ReservaRequest(1L, FECHA, LocalTime.of(18, 0), 2, null, List.of(invitado()),
                List.of(new InsumoPedidoRequest(1L, 4))), SOCIO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSUMO_STOCK_INSUFICIENTE");
    }

    @Test
    void modificar_alMoverElTurno_vuelveAComprobarQueLosInsumosAlcancen() {
        Reserva existente = reservaExistente();
        cancha1Existe();

        servicio.modificar(10L, new ReservaUpdateRequest(null, null, LocalTime.of(19, 0), null), EMPLEADO);

        verify(asignacionDeInsumos).revalidar(existente);
    }

    // ---------- Código de descuento (RF69) ----------

    /** Lo que devolvería el módulo de descuentos para un 25% sobre el total. */
    private void cyberweekDescuenta25(String total, String descontado, String queda) {
        when(descuentoPorCodigo.aplicar("CYBERWEEK", new java.math.BigDecimal(total)))
                .thenReturn(new DescuentoPorCodigo.DescuentoAplicado("CYBERWEEK", "25% de descuento",
                        new java.math.BigDecimal(descontado), new java.math.BigDecimal(queda)));
    }

    private static ReservaRequest pedidoCon(String codigo) {
        return new ReservaRequest(1L, FECHA, LocalTime.of(18, 0), 2, null, List.of(invitado()), null, codigo);
    }

    @Test
    void registrar_conCodigoDeDescuento_bajaElTotalYLaSenia() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();
        cyberweekDescuenta25("16000.00", "4000.00", "12000.00");

        ReservaResponse respuesta = servicio.registrar(pedidoCon("CYBERWEEK"), SOCIO);

        assertThat(respuesta.montoTotal()).isEqualByComparingTo("12000.00");
        assertThat(respuesta.codigoDescuento()).isEqualTo("CYBERWEEK");
        assertThat(respuesta.montoDescuento()).isEqualByComparingTo("4000.00");
        // La seña es el 50% del total, así que baja sola al bajar el total.
        assertThat(respuesta.montoSenia()).isEqualByComparingTo("6000.00");
    }

    @Test
    void registrar_sinCodigo_niSiquieraLePreguntaAlModuloDeDescuentos() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();

        ReservaResponse respuesta = servicio.registrar(pedido(null, invitado()), SOCIO);

        assertThat(respuesta.montoTotal()).isEqualByComparingTo("16000.00");
        assertThat(respuesta.codigoDescuento()).isNull();
        verifyNoInteractions(descuentoPorCodigo);
    }

    @Test
    void registrar_conCodigoEnBlanco_seIgnora() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        guardadoAsignaId();

        assertThat(servicio.registrar(pedidoCon("   "), SOCIO).montoTotal()).isEqualByComparingTo("16000.00");
        verifyNoInteractions(descuentoPorCodigo);
    }

    @Test
    void registrar_conCodigoQueNoExiste_noCreaLaReserva() {
        when(socioRepository.findById(3L)).thenReturn(Optional.of(persona(3L, true)));
        cancha1Existe();
        when(descuentoPorCodigo.aplicar(eq("NOEXISTE"), any()))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.NO_ENCONTRADO, "El código de descuento no existe o no está vigente"));

        // Falla en vez de cobrar el total sin descuento: quien reservó creía tener el beneficio.
        assertThatThrownBy(() -> servicio.registrar(pedidoCon("NOEXISTE"), SOCIO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(reservaRepository, never()).saveAndFlush(any(Reserva.class));
    }

    @Test
    void modificar_conMenosHoras_recalculaElDescuentoSobreElNuevoTotal() {
        Reserva existente = reservaExistente();
        existente.setCodigoDescuento("CYBERWEEK");
        cancha1Existe();
        // Una hora en vez de dos: el total pasa a 8000 y el 25% se recalcula sobre eso.
        cyberweekDescuenta25("8000.00", "2000.00", "6000.00");

        servicio.modificar(10L, new ReservaUpdateRequest(null, null, null, 1), EMPLEADO);

        assertThat(existente.getMontoTotal()).isEqualByComparingTo("6000.00");
        assertThat(existente.getMontoDescuento()).isEqualByComparingTo("2000.00");
    }
}
