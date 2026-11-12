package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.service.InsumoStock;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.reserva.DatosDePrueba.AHORA;
import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CierreDeAlquilerTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private InsumoStock insumoStock;

    private CierreDeAlquiler cierre;

    @BeforeEach
    void setUp() {
        cierre = new CierreDeAlquiler(reservaRepository, reservaInsumoRepository, insumoStock, RELOJ);
    }

    private Reserva con(EstadoReserva estado) {
        Reserva reserva = reserva(10L, estado, HOY, LocalTime.of(9, 0));
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        lenient().when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        lenient().when(reservaRepository.saveAndFlush(reserva)).thenReturn(reserva);
        return reserva;
    }

    private static ReservaInsumo fila(Long insumoId, int cantidad, boolean descontado, boolean repuesto) {
        Insumo insumo = new Insumo();
        insumo.setId(insumoId);
        ReservaInsumo fila = new ReservaInsumo();
        fila.setInsumo(insumo);
        fila.setCantidadAsignada(cantidad);
        fila.setDescontado(descontado);
        fila.setRepuesto(repuesto);
        return fila;
    }

    @Test
    void cerrar_reponeLosInsumosEnUso_yFinalizaLaReserva() {
        Reserva reserva = con(EstadoReserva.EN_CURSO);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(fila(1L, 4, true, false), fila(2L, 12, true, false)));

        Reserva cerrada = cierre.cerrar(10L, 2L);

        assertThat(cerrada.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(cerrada.getFinReal()).isEqualTo(AHORA);
        verify(insumoStock).reponer(1L, 4, "Reserva R-000010: fin del alquiler", 2L);
        verify(insumoStock).reponer(2L, 12, "Reserva R-000010: fin del alquiler", 2L);
        verify(reservaInsumoRepository).marcarRepuestos(10L);
        verify(reservaRepository).saveAndFlush(reserva);
    }

    @Test
    void cerrar_noRepoNiLoQueNuncaSeDescontoNiLoQueYaSeLibero() {
        con(EstadoReserva.EN_CURSO);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(fila(1L, 4, false, false), fila(2L, 3, true, true), fila(3L, 2, true, false)));

        cierre.cerrar(10L, 2L);

        verify(insumoStock).reponer(3L, 2, "Reserva R-000010: fin del alquiler", 2L);
        verify(insumoStock, never()).reponer(eq(1L), anyInt(), anyString(), any());
        verify(insumoStock, never()).reponer(eq(2L), anyInt(), anyString(), any());
    }

    @Test
    void cerrar_unaReservaSinInsumos_finalizaSinMoverStock() {
        con(EstadoReserva.EN_CURSO);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of());

        assertThat(cierre.cerrar(10L, 2L).getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        verify(insumoStock, never()).reponer(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void cerrar_siNoEstaEnCurso_422_yNoRepone() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA, EstadoReserva.FINALIZADA}) {
            con(estado);

            assertThatThrownBy(() -> cierre.cerrar(10L, 2L))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("solo se finaliza un alquiler en curso")
                    .extracting("codigo").isEqualTo("RESERVA_NO_FINALIZABLE");
        }
        verify(insumoStock, never()).reponer(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void cerrar_unaReservaInexistente_404() {
        when(reservaRepository.buscarParaModificar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cierre.cerrar(99L, 2L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cerrarVencida_reponeSinPersonaYConElMotivoDeLaFinalizacionAutomatica() {
        con(EstadoReserva.EN_CURSO);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(fila(1L, 4, true, false)));

        cierre.cerrarVencida(10L);

        verify(insumoStock).reponer(1L, 4, "Reserva R-000010: finalización automática", null);
        verify(reservaInsumoRepository).marcarRepuestos(10L);
    }

    @Test
    void cerrarVencida_siYaNoEstabaEnCurso_noHaceNada() {
        con(EstadoReserva.FINALIZADA);

        cierre.cerrarVencida(10L);

        verify(insumoStock, never()).reponer(anyLong(), anyInt(), anyString(), any());
        verify(reservaRepository, never()).saveAndFlush(any());
    }
}
