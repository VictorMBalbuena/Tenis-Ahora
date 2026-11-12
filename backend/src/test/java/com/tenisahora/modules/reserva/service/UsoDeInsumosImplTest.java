package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsoDeInsumosImplTest {

    @Mock
    private ReservaInsumoRepository reservaInsumoRepository;
    @InjectMocks
    private UsoDeInsumosImpl adaptador;

    @Test
    void enUso_sumaPorInsumoLasUnidadesSinReponer() {
        when(reservaInsumoRepository.sumaEnUso(Set.of(1L, 2L))).thenReturn(List.of(new Object[]{1L, 8L}, new Object[]{2L, 3L}));

        assertThat(adaptador.enUso(Set.of(1L, 2L))).isEqualTo(Map.of(1L, 8, 2L, 3));
    }

    @Test
    void enUso_sinInsumos_noConsultaLaBase() {
        assertThat(adaptador.enUso(Set.of())).isEmpty();
        verify(reservaInsumoRepository, never()).sumaEnUso(Set.of());
    }

    @Test
    void reservasVigentesCon_pideLasConfirmadasYPendientesDePago() {
        LocalDate hoy = LocalDate.of(2026, 10, 19);
        when(reservaInsumoRepository.reservasConInsumo(1L, EstadoReserva.queRetienenHorario(), hoy))
                .thenReturn(List.of(4L, 9L));

        assertThat(adaptador.reservasVigentesCon(1L, hoy)).containsExactly(4L, 9L);
    }
}
