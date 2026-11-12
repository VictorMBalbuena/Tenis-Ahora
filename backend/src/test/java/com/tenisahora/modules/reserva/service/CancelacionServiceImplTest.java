package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.CancelacionResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.tenisahora.modules.reserva.DatosDePrueba.AHORA;
import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelacionServiceImplTest {

    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private AccesoAReservas acceso;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private ArmadoDeRespuestas respuestas;

    private CancelacionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        PoliticaPorPlazo politica = new PoliticaPorPlazo(PROPIEDADES, new CalculadoraDeMontos(PROPIEDADES), id -> new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(new java.math.BigDecimal("8000.00"), new java.math.BigDecimal("8000.00")));
        servicio = new CancelacionServiceImpl(acceso, politica, reservaRepository, reservaInsumoRepository, respuestas, PROPIEDADES, RELOJ, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    /** Una reserva que empieza dentro de la cantidad de horas indicada. */
    private Reserva empezandoEn(EstadoReserva estado, long horas) {
        java.time.LocalDateTime inicio = AHORA.plusHours(horas);
        return reserva(10L, estado, inicio.toLocalDate(), inicio.toLocalTime());
    }

    private static ReservaResponse respuestaVacia() {
        return new ReservaResponse(10L, "R-000010", 1L, "Cancha 1", HOY, LocalTime.of(18, 0), LocalTime.of(20, 0), EstadoReserva.CANCELADA,
                2, List.of(), null, null, null, null, null, null, null, null, List.of(), null, null, null, null, null);
    }

    // ---------- Vista previa ----------

    @Test
    void previsualizar_confirmadaATiempo_diceQueSeReembolsa() {
        when(acceso.paraCancelar(10L, SOCIO, false)).thenReturn(empezandoEn(EstadoReserva.CONFIRMADA, 30));

        CancelacionResponse vista = servicio.previsualizar(10L, SOCIO);

        assertThat(vista.aTiempo()).isTrue();
        assertThat(vista.montoReembolso()).isEqualByComparingTo("8000.00");
        assertThat(vista.mensaje()).contains("reembolso").contains("30 horas");
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void previsualizar_confirmadaTardia_diceQueSePierdeLaSenia_A1() {
        when(acceso.paraCancelar(10L, EMPLEADO, false)).thenReturn(empezandoEn(EstadoReserva.CONFIRMADA, 3));

        CancelacionResponse vista = servicio.previsualizar(10L, EMPLEADO);

        assertThat(vista.aTiempo()).isFalse();
        assertThat(vista.montoPenalidad()).isEqualByComparingTo("8000.00");
        assertThat(vista.mensaje()).contains("menos de 6 horas").contains("se pierde la seña");
    }

    @Test
    void previsualizar_pendienteDePago_seCancelaSinCargo() {
        when(acceso.paraCancelar(10L, SOCIO, false)).thenReturn(empezandoEn(EstadoReserva.PENDIENTE_PAGO, 2));

        assertThat(servicio.previsualizar(10L, SOCIO).mensaje()).contains("sin cargo ni reembolso");
    }

    // ---------- Cancelación ----------

    @Test
    void cancelar_aTiempo_dejaElReembolso_liberaLosInsumosYGuardaCuandoFue() {
        Reserva reserva = empezandoEn(EstadoReserva.CONFIRMADA, 30);
        when(acceso.paraCancelar(10L, SOCIO, true)).thenReturn(reserva);
        when(respuestas.deReserva(reserva)).thenReturn(respuestaVacia());

        ReservaResponse respuesta = servicio.cancelar(10L, SOCIO);

        assertThat(respuesta.numero()).isEqualTo("R-000010");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reserva.getFechaCancelacion()).isEqualTo(AHORA);
        assertThat(reserva.getMontoReembolso()).isEqualByComparingTo("8000.00");
        assertThat(reserva.getMontoPenalidad()).isEqualByComparingTo("0");
        verify(reservaInsumoRepository).liberarDeReserva(10L);
        verify(reservaRepository).saveAndFlush(reserva);
    }

    @Test
    void cancelar_tardia_dejaLaPenalidadYNingunReembolso_RD12() {
        Reserva reserva = empezandoEn(EstadoReserva.CONFIRMADA, 2);
        when(acceso.paraCancelar(10L, EMPLEADO, true)).thenReturn(reserva);
        when(respuestas.deReserva(reserva)).thenReturn(respuestaVacia());

        servicio.cancelar(10L, EMPLEADO);

        assertThat(reserva.getMontoPenalidad()).isEqualByComparingTo("8000.00");
        assertThat(reserva.getMontoReembolso()).isEqualByComparingTo("0");
    }

    @Test
    void cancelar_unaReservaYaCanceladaOFinalizada_422YNoTocaNada() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.CANCELADA, EstadoReserva.FINALIZADA}) {
            when(acceso.paraCancelar(10L, SOCIO, true)).thenReturn(empezandoEn(estado, 30));

            assertThatThrownBy(() -> servicio.cancelar(10L, SOCIO))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .extracting("codigo").isEqualTo("RESERVA_NO_CANCELABLE");
        }
        verify(reservaInsumoRepository, never()).liberarDeReserva(anyLong());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelar_siElAccesoFalla_nadaSeToca() {
        when(acceso.paraCancelar(10L, SOCIO, true)).thenThrow(new AccessDeniedException("Solo el titular puede cancelar la reserva"));

        assertThatThrownBy(() -> servicio.cancelar(10L, SOCIO)).isInstanceOf(AccessDeniedException.class);
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelar_unaReservaQueNoExiste_404() {
        when(acceso.paraCancelar(99L, EMPLEADO, true)).thenThrow(
                new RecursoNoEncontradoException(com.tenisahora.shared.exception.CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));

        assertThatThrownBy(() -> servicio.cancelar(99L, EMPLEADO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- Vencimiento de pendientes ----------

    @Test
    void cancelarVencidas_cancelaLasPendientesSinCargoYLiberaSusInsumos() {
        Reserva una = empezandoEn(EstadoReserva.PENDIENTE_PAGO, 48);
        Reserva otra = empezandoEn(EstadoReserva.PENDIENTE_PAGO, 72);
        otra.setId(11L);
        when(reservaRepository.creadasAntesDe(EstadoReserva.PENDIENTE_PAGO, AHORA.minusMinutes(30))).thenReturn(List.of(una, otra));

        int canceladas = servicio.cancelarVencidas();

        assertThat(canceladas).isEqualTo(2);
        assertThat(List.of(una, otra)).allSatisfy(reserva -> {
            assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(reserva.getFechaCancelacion()).isEqualTo(AHORA);
            assertThat(reserva.getMontoPenalidad()).isEqualByComparingTo("0");
            assertThat(reserva.getMontoReembolso()).isEqualByComparingTo("0");
        });
        verify(reservaInsumoRepository).liberarDeReserva(10L);
        verify(reservaInsumoRepository).liberarDeReserva(11L);
    }

    @Test
    void cancelarVencidas_sinVencidas_noHaceNada() {
        when(reservaRepository.creadasAntesDe(EstadoReserva.PENDIENTE_PAGO, AHORA.minusMinutes(30))).thenReturn(List.of());

        assertThat(servicio.cancelarVencidas()).isZero();
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void elDiaDeLaReservaCuenta_unaDeManianaALas9EstaATiempo() {
        Reserva reserva = reserva(10L, EstadoReserva.CONFIRMADA, LocalDate.of(2026, 10, 21), LocalTime.of(9, 0));
        when(acceso.paraCancelar(10L, SOCIO, false)).thenReturn(reserva);

        assertThat(servicio.previsualizar(10L, SOCIO).aTiempo()).isTrue();
    }
}
