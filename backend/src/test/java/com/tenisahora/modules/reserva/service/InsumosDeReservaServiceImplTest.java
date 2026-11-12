package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.AsignacionDeInsumosRequest;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.dto.InsumoPedidoRequest;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.List;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsumosDeReservaServiceImplTest {

    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final AsignacionDeInsumosRequest PEDIDO = new AsignacionDeInsumosRequest(List.of(new InsumoPedidoRequest(1L, 4)));

    @Mock
    private AccesoAReservas acceso;
    @Mock
    private AsignacionDeInsumos asignacion;
    @Mock
    private DisponibilidadDeInsumos disponibilidad;
    @Mock
    private ArmadoDeRespuestas respuestas;

    private InsumosDeReservaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new InsumosDeReservaServiceImpl(acceso, asignacion, disponibilidad, respuestas, new ReglasDeReserva(PROPIEDADES, RELOJ));
    }

    private Reserva deEstado(EstadoReserva estado, Actor actor) {
        Reserva reserva = reserva(10L, estado, HOY.plusDays(2), LocalTime.of(18, 0));
        when(acceso.paraGestionar(10L, actor, true)).thenReturn(reserva);
        return reserva;
    }

    @Test
    void asignar_alUnaReservaPendienteOConfirmada_reemplazaLosInsumos() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA}) {
            Reserva reserva = deEstado(estado, SOCIO);
            ReservaResponse respuesta = org.mockito.Mockito.mock(ReservaResponse.class);
            when(respuestas.deReserva(reserva)).thenReturn(respuesta);

            assertThat(servicio.asignar(10L, PEDIDO, SOCIO)).isSameAs(respuesta);
            verify(asignacion).asignar(reserva, PEDIDO.insumos());
        }
    }

    @Test
    void asignar_unaReservaQueYaEmpezoOEstaCancelada_422() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.EN_CURSO, EstadoReserva.CANCELADA, EstadoReserva.FINALIZADA}) {
            deEstado(estado, EMPLEADO);

            assertThatThrownBy(() -> servicio.asignar(10L, PEDIDO, EMPLEADO))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("ya no se le pueden cambiar los insumos")
                    .extracting("codigo").isEqualTo("RESERVA_NO_MODIFICABLE");
        }
        verify(asignacion, never()).asignar(any(), any());
    }

    @Test
    void asignar_siElAccesoFalla_nadaSeAsigna() {
        when(acceso.paraGestionar(10L, SOCIO, true)).thenThrow(new AccessDeniedException("Solo el titular puede cancelar la reserva"));
        when(acceso.paraGestionar(11L, SOCIO, true))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));

        assertThatThrownBy(() -> servicio.asignar(10L, PEDIDO, SOCIO)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.asignar(11L, PEDIDO, SOCIO)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(asignacion, never()).asignar(any(), any());
    }

    @Test
    void disponibles_validaElTurnoYDevuelveLoQueSePuedeAsignar() {
        InsumoDisponibleResponse raquetas = new InsumoDisponibleResponse(1L, "Raquetas", null, 10, 6);
        when(disponibilidad.paraElTurno(HOY.plusDays(2), new com.tenisahora.modules.cancha.service.FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(20, 0))))
                .thenReturn(List.of(raquetas));

        assertThat(servicio.disponibles(HOY.plusDays(2), LocalTime.of(18, 0), 2)).containsExactly(raquetas);
    }

    @Test
    void disponibles_deUnTurnoQueNoSePuedeReservar_fallaConLaReglaDeReservas() {
        assertThatThrownBy(() -> servicio.disponibles(HOY.plusDays(45), LocalTime.of(18, 0), 2))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_FUERA_DE_PLAZO");
    }
}
