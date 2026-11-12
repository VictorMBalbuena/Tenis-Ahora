package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.LimitesDeStock;
import com.tenisahora.modules.insumo.dto.MovimientoRequest;
import com.tenisahora.modules.insumo.dto.MovimientoResponse;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.MovimientoStock;
import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.modules.insumo.mapper.InsumoMapper;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.modules.insumo.repository.MovimientoStockRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsumoStockImplTest {

    private static final long INSUMO = 1L;
    private static final long ACTOR = 2L;

    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private MovimientoStockRepository movimientoRepository;
    @Mock
    private RegistroDeMovimientos movimientos;
    @Mock
    private UsoDeInsumos usoDeInsumos;

    private InsumoStockImpl stock;

    @BeforeEach
    void setUp() {
        stock = new InsumoStockImpl(insumoRepository, movimientoRepository, new InsumoPorId(insumoRepository), new InsumoMapper(),
                movimientos, usoDeInsumos);
    }

    private static Insumo insumo(int cantidad) {
        Insumo insumo = new Insumo();
        insumo.setId(INSUMO);
        insumo.setNombre("Raquetas");
        insumo.setCategoria(new CategoriaInsumo("RAQUETA"));
        insumo.setCantidadDisponible(cantidad);
        insumo.setCantidadMinima(6);
        insumo.setActivo(true);
        return insumo;
    }

    // ---------- CU14 ----------

    @Test
    void descontar_restaYAsientaElMovimientoConElStockQueQuedo() {
        when(insumoRepository.descontar(INSUMO, 4)).thenReturn(1);
        Insumo actual = insumo(20);
        when(insumoRepository.findByIdAndActivoTrue(INSUMO)).thenReturn(Optional.of(actual));
        when(usoDeInsumos.enUso(Set.of(INSUMO))).thenReturn(Map.of(INSUMO, 8));

        InsumoResponse respuesta = stock.descontar(INSUMO, 4, "Rotura", ACTOR);

        assertThat(respuesta.cantidadDisponible()).isEqualTo(20);
        assertThat(respuesta.enUso()).isEqualTo(8);
        verify(movimientos).registrar(actual, TipoMovimiento.DESCUENTO, -4, 20, "Rotura", ACTOR);
    }

    @Test
    void descontar_masDeLoQueHay_422DiceCuantasHayYNoAsientaNada_A1() {
        when(insumoRepository.descontar(INSUMO, 30)).thenReturn(0);
        when(insumoRepository.findByIdAndActivoTrue(INSUMO)).thenReturn(Optional.of(insumo(3)));

        assertThatThrownBy(() -> stock.descontar(INSUMO, 30, null, ACTOR))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Solo hay 3 unidades")
                .hasMessageContaining("Raquetas")
                .extracting("codigo").isEqualTo("INSUMO_STOCK_INSUFICIENTE");
        verify(movimientos, never()).registrar(any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void descontar_insumoInexistente_404() {
        when(insumoRepository.descontar(99L, 1)).thenReturn(0);
        when(insumoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stock.descontar(99L, 1, null, ACTOR))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("INSUMO_NO_ENCONTRADO");
    }

    @Test
    void descontar_conCantidadFueraDeRango_400SinTocarLaBase() {
        assertThatThrownBy(() -> stock.descontar(INSUMO, 0, null, ACTOR)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> stock.descontar(INSUMO, -5, null, ACTOR)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> stock.descontar(INSUMO, LimitesDeStock.MAXIMO_POR_MOVIMIENTO + 1, null, ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("MOVIMIENTO_INVALIDO");
        verify(insumoRepository, never()).descontar(any(), anyInt());
    }

    // ---------- CU15 ----------

    @Test
    void reponer_sumaYAsientaElMovimiento() {
        when(insumoRepository.reponer(INSUMO, 5, LimitesDeStock.MAXIMO_STOCK - 5)).thenReturn(1);
        Insumo actual = insumo(25);
        when(insumoRepository.findByIdAndActivoTrue(INSUMO)).thenReturn(Optional.of(actual));

        InsumoResponse respuesta = stock.reponer(INSUMO, 5, "Compra", ACTOR);

        assertThat(respuesta.cantidadDisponible()).isEqualTo(25);
        verify(movimientos).registrar(actual, TipoMovimiento.REPOSICION, 5, 25, "Compra", ACTOR);
    }

    @Test
    void reponer_pasandoseDelMaximo_422YNoAsientaNada() {
        when(insumoRepository.reponer(INSUMO, 10, LimitesDeStock.MAXIMO_STOCK - 10)).thenReturn(0);
        when(insumoRepository.findByIdAndActivoTrue(INSUMO)).thenReturn(Optional.of(insumo(LimitesDeStock.MAXIMO_STOCK)));

        assertThatThrownBy(() -> stock.reponer(INSUMO, 10, null, ACTOR))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("MOVIMIENTO_INVALIDO");
        verify(movimientos, never()).registrar(any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void reponer_insumoInexistente_404() {
        when(insumoRepository.reponer(99L, 1, LimitesDeStock.MAXIMO_STOCK - 1)).thenReturn(0);
        when(insumoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stock.reponer(99L, 1, null, ACTOR)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- mover (endpoint) ----------

    @Test
    void mover_conDescuentoYConReposicion_llamaAlQueCorresponde() {
        when(insumoRepository.descontar(INSUMO, 2)).thenReturn(1);
        when(insumoRepository.reponer(INSUMO, 3, LimitesDeStock.MAXIMO_STOCK - 3)).thenReturn(1);
        when(insumoRepository.findByIdAndActivoTrue(INSUMO)).thenReturn(Optional.of(insumo(10)));

        stock.mover(INSUMO, new MovimientoRequest(TipoMovimiento.DESCUENTO, 2, "Uso"), ACTOR);
        stock.mover(INSUMO, new MovimientoRequest(TipoMovimiento.REPOSICION, 3, "Compra"), ACTOR);

        verify(insumoRepository).descontar(INSUMO, 2);
        verify(insumoRepository).reponer(INSUMO, 3, LimitesDeStock.MAXIMO_STOCK - 3);
    }

    @Test
    void mover_conAjuste_400PorqueElAjusteSeHaceModificandoElInsumo() {
        assertThatThrownBy(() -> stock.mover(INSUMO, new MovimientoRequest(TipoMovimiento.AJUSTE, 2, null), ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("MOVIMIENTO_INVALIDO");
        verify(insumoRepository, never()).descontar(any(), anyInt());
    }

    // ---------- rastro ----------

    @Test
    void historial_devuelveLosMovimientosDelInsumo() {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setId(15L);
        movimiento.setTipo(TipoMovimiento.DESCUENTO);
        movimiento.setVariacion(-4);
        movimiento.setStockResultante(20);
        movimiento.setMotivo("Rotura");
        movimiento.setFecha(LocalDateTime.of(2026, 10, 19, 10, 0));
        movimiento.setPersonaId(ACTOR);
        when(insumoRepository.findByIdAndActivoTrue(INSUMO)).thenReturn(Optional.of(insumo(20)));
        when(movimientoRepository.findByInsumoIdOrderByFechaDescIdDesc(org.mockito.ArgumentMatchers.eq(INSUMO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento)));

        PageResponse<MovimientoResponse> respuesta = stock.historial(INSUMO, 0, 20);

        assertThat(respuesta.contenido()).containsExactly(new MovimientoResponse(15L, TipoMovimiento.DESCUENTO, -4, 20, "Rotura",
                LocalDateTime.of(2026, 10, 19, 10, 0), ACTOR));
    }

    @Test
    void historial_deUnInsumoInexistente_404() {
        when(insumoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stock.historial(99L, 0, 20)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
