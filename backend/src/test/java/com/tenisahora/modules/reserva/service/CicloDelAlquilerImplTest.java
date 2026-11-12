package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.service.InsumoStock;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.reserva.DatosDePrueba.AHORA;
import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CicloDelAlquilerImplTest {

    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private InsumoStock insumoStock;
    @Mock
    private CierreDeAlquiler cierre;
    @Mock
    private ArmadoDeRespuestas respuestas;

    private CicloDelAlquilerImpl ciclo;

    @BeforeEach
    void setUp() {
        ciclo = new CicloDelAlquilerImpl(reservaRepository, reservaInsumoRepository, insumoStock, cierre, respuestas, PROPIEDADES, RELOJ);
    }

    /** Una reserva de hoy que empieza a la hora indicada y dura 2 horas. "Ahora" son las 12:00. */
    private Reserva deHoyA(int hora, EstadoReserva estado) {
        Reserva reserva = reserva(10L, estado, HOY, LocalTime.of(hora, 0));
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        return reserva;
    }

    private static ReservaInsumo asignado(Long insumoId, int cantidad) {
        Insumo insumo = new Insumo();
        insumo.setId(insumoId);
        ReservaInsumo asignado = new ReservaInsumo();
        asignado.setInsumo(insumo);
        asignado.setCantidadAsignada(cantidad);
        asignado.setDescontado(false);
        asignado.setRepuesto(false);
        return asignado;
    }

    private void iniciaConRespuesta(Reserva reserva) {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(respuestas.deReserva(reserva)).thenReturn(respuestaVacia());
    }

    private static ReservaResponse respuestaVacia() {
        return new ReservaResponse(10L, "R-000010", 1L, "Cancha 1", HOY, LocalTime.of(12, 0), LocalTime.of(14, 0), EstadoReserva.EN_CURSO,
                2, List.of(), null, null, null, null, null, null, null, null, List.of(), null, null, null, null, null);
    }

    // ---------- CU14, iniciar ----------

    @Test
    void iniciar_descuentaCadaInsumoAsignado_yPasaLaReservaAEnCurso() {
        Reserva reserva = deHoyA(12, EstadoReserva.CONFIRMADA);
        iniciaConRespuesta(reserva);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(asignado(1L, 4), asignado(2L, 12)));

        ciclo.iniciar(10L, EMPLEADO);

        InOrder orden = inOrder(insumoStock, reservaInsumoRepository, reservaRepository);
        orden.verify(insumoStock).descontar(1L, 4, "Reserva R-000010: inicio del alquiler", 2L);
        orden.verify(insumoStock).descontar(2L, 12, "Reserva R-000010: inicio del alquiler", 2L);
        orden.verify(reservaInsumoRepository).marcarDescontados(10L);
        orden.verify(reservaRepository).saveAndFlush(reserva);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
        assertThat(reserva.getInicioReal()).isEqualTo(AHORA);
    }

    @Test
    void iniciar_unaReservaSinInsumos_iniciaSinMoverStock() {
        Reserva reserva = deHoyA(12, EstadoReserva.CONFIRMADA);
        iniciaConRespuesta(reserva);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of());

        ciclo.iniciar(10L, EMPLEADO);

        verify(insumoStock, never()).descontar(anyLong(), anyInt(), anyString(), any());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
    }

    @Test
    void iniciar_ignoraLosInsumosYaDescontadosOLiberados() {
        Reserva reserva = deHoyA(12, EstadoReserva.CONFIRMADA);
        iniciaConRespuesta(reserva);
        ReservaInsumo yaDescontado = asignado(1L, 4);
        yaDescontado.setDescontado(true);
        ReservaInsumo liberado = asignado(2L, 4);
        liberado.setRepuesto(true);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(yaDescontado, liberado, asignado(3L, 2)));

        ciclo.iniciar(10L, EMPLEADO);

        verify(insumoStock).descontar(3L, 2, "Reserva R-000010: inicio del alquiler", 2L);
        verify(insumoStock, never()).descontar(eq(1L), anyInt(), anyString(), any());
        verify(insumoStock, never()).descontar(eq(2L), anyInt(), anyString(), any());
    }

    @Test
    void iniciar_conStockInsuficienteEnUnInsumo_fallaTodo_yLaReservaSigueConfirmada() {
        Reserva reserva = deHoyA(12, EstadoReserva.CONFIRMADA);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(asignado(1L, 4), asignado(2L, 12)));
        lenient().doThrow(new ReglaDeNegocioException(CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay 3 unidades disponibles de Pelotas"))
                .when(insumoStock).descontar(eq(2L), anyInt(), anyString(), any());

        assertThatThrownBy(() -> ciclo.iniciar(10L, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSUMO_STOCK_INSUFICIENTE");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reserva.getInicioReal()).isNull();
        verify(reservaRepository, never()).saveAndFlush(any());
        verify(reservaInsumoRepository, never()).marcarDescontados(anyLong());
    }

    @Test
    void iniciar_unaReservaPendienteDePagoOCancelada_422() {
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA}) {
            deHoyA(12, estado);

            assertThatThrownBy(() -> ciclo.iniciar(10L, EMPLEADO))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("solo se inicia una reserva confirmada")
                    .extracting("codigo").isEqualTo("RESERVA_NO_INICIABLE");
        }
        verify(insumoStock, never()).descontar(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void iniciar_conMasDeMediaHoraDeAnticipacion_422_perodentroDeLaVentanaSePermite() {
        deHoyA(13, EstadoReserva.CONFIRMADA);
        assertThatThrownBy(() -> ciclo.iniciar(10L, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("30 minutos antes")
                .extracting("codigo").isEqualTo("RESERVA_NO_INICIABLE");
    }

    @Test
    void iniciar_unTurnoQueYaTermino_422() {
        deHoyA(9, EstadoReserva.CONFIRMADA);

        assertThatThrownBy(() -> ciclo.iniciar(10L, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);
        verify(insumoStock, never()).descontar(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void iniciar_unTurnoDeOtroDia_422() {
        Reserva manana = reserva(10L, EstadoReserva.CONFIRMADA, LocalDate.of(2026, 10, 20), LocalTime.of(12, 0));
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(manana));

        assertThatThrownBy(() -> ciclo.iniciar(10L, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void iniciar_unaReservaInexistente_404() {
        when(reservaRepository.buscarParaModificar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ciclo.iniciar(99L, EMPLEADO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- CU15, finalizar ----------

    @Test
    void finalizar_lePideAlCierreQueRepongaYDevuelveLaReserva() {
        Reserva finalizada = reserva(10L, EstadoReserva.FINALIZADA, HOY, LocalTime.of(10, 0));
        when(cierre.cerrar(10L, 2L)).thenReturn(finalizada);
        when(respuestas.deReserva(finalizada)).thenReturn(respuestaVacia());

        assertThat(ciclo.finalizar(10L, EMPLEADO).numero()).isEqualTo("R-000010");
        verify(cierre).cerrar(10L, 2L);
    }

    @Test
    void finalizar_siNoEstaEnCurso_laExcepcionDelCierreSePropaga() {
        when(cierre.cerrar(10L, 2L)).thenThrow(new ReglaDeNegocioException(CodigoError.RESERVA_NO_FINALIZABLE, "no está en curso"));

        assertThatThrownBy(() -> ciclo.finalizar(10L, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_NO_FINALIZABLE");
    }

    // ---------- finalización automática ----------

    @Test
    void finalizarVencidas_cierraCadaReservaQuePasoDelMargen_yTolera_queUnaFalle() {
        Reserva una = reserva(10L, EstadoReserva.EN_CURSO, HOY, LocalTime.of(8, 0));
        Reserva otra = reserva(11L, EstadoReserva.EN_CURSO, HOY, LocalTime.of(9, 0));
        Reserva tercera = reserva(12L, EstadoReserva.EN_CURSO, HOY, LocalTime.of(9, 30));
        when(reservaRepository.terminadasAntesDe(EstadoReserva.EN_CURSO, HOY, LocalTime.of(11, 30))).thenReturn(List.of(una, otra, tercera));
        lenient().doThrow(new IllegalStateException("falla de la base")).when(cierre).cerrarVencida(11L);

        int finalizadas = ciclo.finalizarVencidas();

        assertThat(finalizadas).isEqualTo(2);
        verify(cierre).cerrarVencida(10L);
        verify(cierre).cerrarVencida(11L);
        verify(cierre).cerrarVencida(12L);
    }

    @Test
    void finalizarVencidas_sinReservasVencidas_noHaceNada() {
        when(reservaRepository.terminadasAntesDe(EstadoReserva.EN_CURSO, HOY, LocalTime.of(11, 30))).thenReturn(List.of());

        assertThat(ciclo.finalizarVencidas()).isZero();
        verify(cierre, never()).cerrarVencida(anyLong());
    }
}
