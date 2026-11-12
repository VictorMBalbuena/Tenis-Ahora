package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.dto.EstadoFranja;
import com.tenisahora.modules.cancha.dto.FranjaResponse;
import com.tenisahora.modules.cancha.dto.GrillaResponse;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.service.DisponibilidadService;
import com.tenisahora.modules.reserva.dto.CanchaDisponibleResponse;
import com.tenisahora.modules.reserva.dto.DisponibilidadReservaResponse;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.reserva.DatosDePrueba.cancha;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadParaReservarImplTest {

    private static final LocalDate FECHA = HOY.plusDays(2);

    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private DisponibilidadService disponibilidadService;

    private DisponibilidadParaReservarImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new DisponibilidadParaReservarImpl(canchaRepository, disponibilidadService, new ReglasDeReserva(PROPIEDADES, RELOJ), PROPIEDADES);
    }

    /** Franjas de una hora desde las 8 hasta las 12, todas libres salvo la de las 9, que está ocupada. */
    private GrillaResponse grilla(Long canchaId) {
        List<FranjaResponse> franjas = new ArrayList<>();
        for (int hora = 8; hora < 12; hora++) {
            franjas.add(new FranjaResponse(FECHA, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0),
                    hora == 9 ? EstadoFranja.OCUPADA : EstadoFranja.LIBRE));
        }
        return new GrillaResponse(canchaId, FECHA, FECHA, franjas);
    }

    @Test
    void consultar_traeLaGrillaDeCadaCanchaActiva_yMarcaQueSePuedeReservar() {
        when(canchaRepository.findByActivaTrueOrderByIdentificador()).thenReturn(List.of(cancha(1L, "Cancha 1", "8000"), cancha(2L, "Cancha 2", "6000")));
        when(disponibilidadService.consultar(1L, FECHA, FECHA)).thenReturn(grilla(1L));
        when(disponibilidadService.consultar(2L, FECHA, FECHA)).thenReturn(grilla(2L));

        DisponibilidadReservaResponse respuesta = servicio.consultar(FECHA, null, null, null);

        assertThat(respuesta.reservable()).isTrue();
        assertThat(respuesta.aviso()).isNull();
        assertThat(respuesta.canchas()).extracting(CanchaDisponibleResponse::identificador).containsExactly("Cancha 1", "Cancha 2");
        assertThat(respuesta.canchas().get(0).franjas()).hasSize(4);
        assertThat(respuesta.canchas().get(0).precioPorHora()).isEqualByComparingTo("8000");
    }

    @Test
    void consultar_conRangoHorario_recortaLasFranjas() {
        when(canchaRepository.findByActivaTrueOrderByIdentificador()).thenReturn(List.of(cancha(1L, "Cancha 1", "8000")));
        when(disponibilidadService.consultar(1L, FECHA, FECHA)).thenReturn(grilla(1L));

        DisponibilidadReservaResponse respuesta = servicio.consultar(FECHA, LocalTime.of(9, 0), LocalTime.of(11, 0), null);

        assertThat(respuesta.canchas().get(0).franjas()).extracting(FranjaResponse::desde)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThat(respuesta.canchas().get(0).franjas().get(0).estado()).isEqualTo(EstadoFranja.OCUPADA);
    }

    @Test
    void consultar_porSuperficie_pideSoloLasDeEsaSuperficie() {
        when(canchaRepository.buscarActivasPorSuperficie("CEMENTO")).thenReturn(List.of());

        assertThat(servicio.consultar(FECHA, null, null, "CEMENTO").canchas()).isEmpty();
        verify(canchaRepository, never()).findByActivaTrueOrderByIdentificador();
    }

    @Test
    void consultar_masAllaDeLos30Dias_sePuedeMirarPeroNoReservar_A1() {
        LocalDate lejos = HOY.plusDays(45);
        when(canchaRepository.findByActivaTrueOrderByIdentificador()).thenReturn(List.of(cancha(1L, "Cancha 1", "8000")));
        when(disponibilidadService.consultar(1L, lejos, lejos)).thenReturn(grilla(1L));

        DisponibilidadReservaResponse respuesta = servicio.consultar(lejos, null, null, null);

        assertThat(respuesta.reservable()).isFalse();
        assertThat(respuesta.aviso()).contains("30 días").contains("solo de consulta");
        assertThat(respuesta.canchas()).hasSize(1);
    }

    @Test
    void consultar_conUnaSolaHoraOConElFinAntesDelInicio_400() {
        assertThatThrownBy(() -> servicio.consultar(FECHA, LocalTime.of(9, 0), null, null))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RANGO_DE_CONSULTA_INVALIDO");
        assertThatThrownBy(() -> servicio.consultar(FECHA, null, LocalTime.of(9, 0), null)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.consultar(FECHA, LocalTime.of(11, 0), LocalTime.of(9, 0), null)).isInstanceOf(SolicitudInvalidaException.class);
    }
}
