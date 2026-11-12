package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.service.ConsultaDeStock;
import com.tenisahora.modules.insumo.service.ConsultaDeStock.StockLibre;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadDeInsumosTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 22);
    private static final FranjaHoraria TURNO = new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(20, 0));

    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private ConsultaDeStock consultaDeStock;

    private DisponibilidadDeInsumos disponibilidad;

    @BeforeEach
    void setUp() {
        disponibilidad = new DisponibilidadDeInsumos(reservaInsumoRepository, consultaDeStock);
    }

    private static StockLibre stock(Long id, String nombre, int libres) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre);
        insumo.setCategoria(new CategoriaInsumo("PELOTA"));
        insumo.setCantidadDisponible(libres);
        return new StockLibre(insumo, libres);
    }

    private static StockLibre stockConEnUso(Long id, String nombre, int libres, int enUso) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre);
        insumo.setCategoria(new CategoriaInsumo("RED"));
        insumo.setCantidadDisponible(libres);
        return new StockLibre(insumo, libres, libres + enUso);
    }

    @Test
    void comprometidos_sumaLoPrometidoPorInsumoALosTurnosQueSePisan() {
        when(reservaInsumoRepository.comprometidos(Set.of(1L, 2L), EstadoReserva.queRetienenHorario(), DIA,
                LocalTime.of(18, 0), LocalTime.of(20, 0), 0L)).thenReturn(List.of(new Object[]{1L, 6L}, new Object[]{2L, 2L}));

        assertThat(disponibilidad.comprometidos(Set.of(1L, 2L), DIA, TURNO, null)).containsEntry(1L, 6).containsEntry(2L, 2);
    }

    @Test
    void comprometidos_alReasignarUnaReserva_laExcluyeDeLaCuenta() {
        when(reservaInsumoRepository.comprometidos(Set.of(1L), EstadoReserva.queRetienenHorario(), DIA,
                LocalTime.of(18, 0), LocalTime.of(20, 0), 55L)).thenReturn(List.of());

        assertThat(disponibilidad.comprometidos(Set.of(1L), DIA, TURNO, 55L)).isEmpty();
        verify(reservaInsumoRepository).comprometidos(Set.of(1L), EstadoReserva.queRetienenHorario(), DIA, LocalTime.of(18, 0), LocalTime.of(20, 0), 55L);
    }

    @Test
    void comprometidos_sinInsumos_noConsultaLaBase() {
        assertThat(disponibilidad.comprometidos(Set.of(), DIA, TURNO, null)).isEmpty();
        verify(reservaInsumoRepository, never()).comprometidos(any(), any(), any(), any(), any(), any());
    }

    @Test
    void paraElTurno_diceCuantasUnidadesSePuedenAsignarDeCadaInsumo() {
        when(consultaDeStock.activos()).thenReturn(List.of(stock(1L, "Pelotas", 40), stock(2L, "Raquetas", 10)));
        when(reservaInsumoRepository.comprometidos(any(), any(), any(), any(), any(), any())).thenReturn(List.<Object[]>of(new Object[]{2L, 4L}));

        List<InsumoDisponibleResponse> disponibles = disponibilidad.paraElTurno(DIA, TURNO);

        assertThat(disponibles).extracting(InsumoDisponibleResponse::nombre).containsExactly("Pelotas", "Raquetas");
        assertThat(disponibles).extracting(InsumoDisponibleResponse::cantidadLibre).containsExactly(40, 10);
        assertThat(disponibles).extracting(InsumoDisponibleResponse::cantidadAsignable).containsExactly(40, 6);
    }

    @Test
    void paraElTurno_siLoPrometidoSuperaLoLibre_lasAsignablesNuncaSonNegativas() {
        when(consultaDeStock.activos()).thenReturn(List.of(stock(1L, "Pelotas", 5)));
        when(reservaInsumoRepository.comprometidos(any(), any(), any(), any(), any(), any())).thenReturn(List.<Object[]>of(new Object[]{1L, 9L}));

        assertThat(disponibilidad.paraElTurno(DIA, TURNO).get(0).cantidadAsignable()).isZero();
    }

    @Test
    void paraElTurno_sinInsumosActivos_listaVacia() {
        when(consultaDeStock.activos()).thenReturn(List.of());

        assertThat(disponibilidad.paraElTurno(DIA, TURNO)).isEmpty();
    }

    @Test
    void paraElTurno_permiteAsignarHastaElStockTotalSiNoHaySolapamientoEnEseHorario() {
        // En depósito hay 21 redes porque 3 están en uso ahora en la cancha. Total del club: 24.
        when(consultaDeStock.activos()).thenReturn(List.of(stockConEnUso(1L, "Redes", 21, 3)));
        when(reservaInsumoRepository.comprometidos(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        List<InsumoDisponibleResponse> disponibles = disponibilidad.paraElTurno(DIA, TURNO);

        assertThat(disponibles.get(0).cantidadLibre()).isEqualTo(21);
        assertThat(disponibles.get(0).cantidadAsignable()).isEqualTo(24);
    }
}
