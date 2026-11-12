package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.service.ConsultaDeStock;
import com.tenisahora.modules.insumo.service.ConsultaDeStock.StockLibre;
import com.tenisahora.modules.reserva.dto.InsumoPedidoRequest;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionDeInsumosTest {

    @Mock
    private ConsultaDeStock consultaDeStock;
    @Mock
    private DisponibilidadDeInsumos disponibilidad;
    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;

    private AsignacionDeInsumos asignacion;
    private final Reserva reserva = reserva(10L, EstadoReserva.PENDIENTE_PAGO, HOY.plusDays(2), LocalTime.of(18, 0));

    @BeforeEach
    void setUp() {
        asignacion = new AsignacionDeInsumos(consultaDeStock, disponibilidad, reservaInsumoRepository, PROPIEDADES);
    }

    private static Insumo insumo(Long id, String nombre, int libres) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre);
        insumo.setCategoria(new CategoriaInsumo("RAQUETA"));
        insumo.setCantidadDisponible(libres);
        return insumo;
    }

    private void stock(Insumo... insumos) {
        Map<Long, StockLibre> mapa = new java.util.LinkedHashMap<>();
        for (Insumo insumo : insumos) {
            mapa.put(insumo.getId(), new StockLibre(insumo, insumo.getCantidadDisponible()));
        }
        when(consultaDeStock.bloquear(anyCollection())).thenReturn(mapa);
    }

    private void comprometidos(Map<Long, Integer> comprometidos) {
        when(disponibilidad.comprometidos(anyCollection(), eq(HOY.plusDays(2)),
                eq(new com.tenisahora.modules.cancha.service.FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(20, 0))), eq(10L))).thenReturn(comprometidos);
    }

    private static InsumoPedidoRequest pedido(Long insumoId, int cantidad) {
        return new InsumoPedidoRequest(insumoId, cantidad);
    }

    @Test
    void asignar_conStockSuficiente_reemplazaLosInsumosDeLaReserva() {
        stock(insumo(1L, "Raquetas", 10), insumo(2L, "Pelotas", 40));
        comprometidos(Map.of());
        when(reservaInsumoRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ReservaInsumo> asignados = asignacion.asignar(reserva, List.of(pedido(1L, 4), pedido(2L, 12)));

        assertThat(asignados).extracting(a -> a.getInsumo().getId()).containsExactly(1L, 2L);
        assertThat(asignados).extracting(ReservaInsumo::getCantidadAsignada).containsExactly(4, 12);
        assertThat(asignados).allSatisfy(a -> {
            assertThat(a.getDescontado()).isFalse();
            assertThat(a.getRepuesto()).isFalse();
            assertThat(a.getReserva()).isSameAs(reserva);
        });
        verify(reservaInsumoRepository).borrarDeLaReserva(10L);
    }

    @Test
    void asignar_descuentaLoYaPrometidoAOtrasReservasQueSePisan() {
        stock(insumo(1L, "Raquetas", 10));
        comprometidos(Map.of(1L, 3));
        when(reservaInsumoRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(asignacion.asignar(reserva, List.of(pedido(1L, 7)))).hasSize(1);
    }

    @Test
    void asignar_masDeLoQueHayParaEseHorario_422DiceCuantasHay_yNoToca() {
        stock(insumo(1L, "Raquetas", 10));
        comprometidos(Map.of(1L, 3));

        assertThatThrownBy(() -> asignacion.asignar(reserva, List.of(pedido(1L, 8))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Solo hay 7 unidades de Raquetas para ese horario y se pidieron 8")
                .extracting("codigo").isEqualTo("INSUMO_STOCK_INSUFICIENTE");
        verify(reservaInsumoRepository, never()).borrarDeLaReserva(any());
        verify(reservaInsumoRepository, never()).saveAll(anyList());
    }

    @Test
    void asignar_permitePedirMasDeLoDisponibleEnDepositoSiElStockTotalAlcanzaYNoSeSolapa() {
        Insumo redes = insumo(1L, "Redes", 15);
        Map<Long, StockLibre> mapa = new java.util.LinkedHashMap<>();
        mapa.put(1L, new StockLibre(redes, 15, 24));
        when(consultaDeStock.bloquear(anyCollection())).thenReturn(mapa);
        comprometidos(Map.of());
        when(reservaInsumoRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ReservaInsumo> asignados = asignacion.asignar(reserva, List.of(pedido(1L, 18)));

        assertThat(asignados).hasSize(1);
        assertThat(asignados.get(0).getCantidadAsignada()).isEqualTo(18);
    }

    @Test
    void asignar_siTodoEstaPrometido_noQuedaNingunaUnidad() {
        stock(insumo(1L, "Raquetas", 10));
        comprometidos(Map.of(1L, 25));

        assertThatThrownBy(() -> asignacion.asignar(reserva, List.of(pedido(1L, 1))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Solo hay 0 unidades");
    }

    @Test
    void asignar_alFallarUnRenglon_noSeAsignaNingunoTodoONada() {
        stock(insumo(1L, "Raquetas", 10), insumo(2L, "Pelotas", 5));
        comprometidos(Map.of());

        assertThatThrownBy(() -> asignacion.asignar(reserva, List.of(pedido(1L, 2), pedido(2L, 6))))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(reservaInsumoRepository, never()).saveAll(anyList());
    }

    @Test
    void asignar_conPedidoVacioONulo_quitaTodoSinConsultarElStock() {
        when(reservaInsumoRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(asignacion.asignar(reserva, List.of())).isEmpty();
        assertThat(asignacion.asignar(reserva, null)).isEmpty();

        verify(reservaInsumoRepository, org.mockito.Mockito.times(2)).borrarDeLaReserva(10L);
        verify(consultaDeStock, never()).bloquear(anyCollection());
    }

    @Test
    void asignar_conUnInsumoRepetido_400() {
        assertThatThrownBy(() -> asignacion.asignar(reserva, List.of(pedido(1L, 2), pedido(1L, 3))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("más de una vez")
                .extracting("codigo").isEqualTo("RESERVA_INSUMOS_INVALIDOS");
        verify(consultaDeStock, never()).bloquear(anyCollection());
    }

    @Test
    void asignar_masUnidadesQueElTope_400() {
        assertThatThrownBy(() -> asignacion.asignar(reserva, List.of(pedido(1L, 21))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("hasta 20 unidades");
    }

    @Test
    void asignar_masRenglonesQueElTope_400() {
        List<InsumoPedidoRequest> once = new ArrayList<>();
        for (long id = 1; id <= 11; id++) {
            once.add(pedido(id, 1));
        }

        assertThatThrownBy(() -> asignacion.asignar(reserva, once))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("hasta 10 insumos");
    }

    @Test
    void asignar_conUnInsumoInexistenteODadoDeBaja_404_yNoGuardaNada() {
        when(consultaDeStock.bloquear(anyCollection()))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado"));

        assertThatThrownBy(() -> asignacion.asignar(reserva, List.of(pedido(99L, 1))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(reservaInsumoRepository, never()).saveAll(anyList());
    }

    @Test
    void revalidar_conInsumosAsignados_vuelveAComprobarContraElTurnoActual() {
        Insumo raquetas = insumo(1L, "Raquetas", 10);
        ReservaInsumo asignado = new ReservaInsumo();
        asignado.setInsumo(raquetas);
        asignado.setCantidadAsignada(6);
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of(asignado));
        stock(raquetas);
        comprometidos(Map.of(1L, 5));

        assertThatThrownBy(() -> asignacion.revalidar(reserva))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Solo hay 5 unidades de Raquetas");
    }

    @Test
    void revalidar_sinInsumos_noConsultaNada() {
        when(reservaInsumoRepository.deLaReserva(10L)).thenReturn(List.of());

        asignacion.revalidar(reserva);

        verify(consultaDeStock, never()).bloquear(anyCollection());
    }

    @Test
    void asignar_guardaLasFilasQueSeLePasanAlRepositorio() {
        stock(insumo(1L, "Raquetas", 10));
        comprometidos(Map.of());
        when(reservaInsumoRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        asignacion.asignar(reserva, List.of(pedido(1L, 2)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReservaInsumo>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(reservaInsumoRepository).saveAll(guardadas.capture());
        assertThat(guardadas.getValue()).hasSize(1);
    }
}
