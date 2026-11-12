package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.modules.insumo.service.ConsultaDeStock.StockLibre;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaDeStockImplTest {

    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private UsoDeInsumos usoDeInsumos;
    @InjectMocks
    private ConsultaDeStockImpl consulta;

    private static Insumo insumo(Long id, String nombre, int libres) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre);
        insumo.setCantidadDisponible(libres);
        return insumo;
    }

    @Test
    void bloquear_devuelveElStockLibreDeCadaInsumo_pidiendoloEnOrdenDeId() {
        when(insumoRepository.bloquearActivos(Set.of(1L, 2L))).thenReturn(List.of(insumo(1L, "Raquetas", 10), insumo(2L, "Pelotas", 40)));
        when(usoDeInsumos.enUso(Set.of(1L, 2L))).thenReturn(Map.of(1L, 5));

        Map<Long, StockLibre> stock = consulta.bloquear(List.of(2L, 1L, 2L));

        assertThat(stock.keySet()).containsExactly(1L, 2L);
        assertThat(stock.get(1L).cantidadLibre()).isEqualTo(10);
        assertThat(stock.get(1L).stockTotal()).isEqualTo(15);
        assertThat(stock.get(2L).insumo().getNombre()).isEqualTo("Pelotas");
        assertThat(stock.get(2L).stockTotal()).isEqualTo(40);
        verify(insumoRepository).bloquearActivos(Set.of(1L, 2L));
    }

    @Test
    void bloquear_siFaltaUnInsumoODadoDeBaja_404() {
        when(insumoRepository.bloquearActivos(Set.of(1L, 99L))).thenReturn(List.of(insumo(1L, "Raquetas", 10)));

        assertThatThrownBy(() -> consulta.bloquear(List.of(1L, 99L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("INSUMO_NO_ENCONTRADO");
    }

    @Test
    @SuppressWarnings("unchecked")
    void activos_listaLosInsumosActivosConSuStockLibre() {
        when(insumoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(insumo(1L, "Pelotas", 40), insumo(2L, "Redes", 0)));
        when(usoDeInsumos.enUso(Set.of(1L, 2L))).thenReturn(Map.of(2L, 8));

        List<StockLibre> activos = consulta.activos();

        assertThat(activos).extracting(stock -> stock.insumo().getNombre()).containsExactly("Pelotas", "Redes");
        assertThat(activos).extracting(StockLibre::cantidadLibre).containsExactly(40, 0);
        assertThat(activos).extracting(StockLibre::stockTotal).containsExactly(40, 8);
    }
}
