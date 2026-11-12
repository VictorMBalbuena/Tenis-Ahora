package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.BloqueoRequest;
import com.tenisahora.modules.cancha.dto.BloqueoResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.Disponibilidad;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tenisahora.modules.cancha.dto.BloqueoDelDiaResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoServiceImplTest {

    private static final long CANCHA = 1L;
    private static final long ACTOR = 2L;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC);
    private static final FranjaHoraria OCHO_A_DIECIOCHO = new FranjaHoraria(LocalTime.of(8, 0), LocalTime.of(18, 0));

    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Mock
    private ReservasDelRango reservasDelRango;
    @Mock
    private CanchaRepository canchaRepository;

    private BloqueoServiceImpl servicio;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        servicio = new BloqueoServiceImpl(disponibilidadRepository, reservasDelRango, new CanchaPorId(canchaRepository), RELOJ);
        cancha = new Cancha();
        cancha.setId(CANCHA);
        cancha.setActiva(true);
    }

    private void canchaExiste() {
        when(canchaRepository.findById(CANCHA)).thenReturn(Optional.of(cancha));
    }

    private static BloqueoRequest pedido(LocalDate desde, LocalDate hasta) {
        return new BloqueoRequest(desde, hasta, LocalTime.of(8, 0), LocalTime.of(18, 0), "  Cambio de superficie ");
    }

    @Test
    void bloquear_sinReservas_guardaUnaFilaDeMantenimientoPorDia() {
        canchaExiste();
        when(disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(CANCHA, EstadoDisponibilidad.MANTENIMIENTO, HOY.plusDays(1), HOY.plusDays(3)))
                .thenReturn(List.of());
        when(reservasDelRango.idsVigentesEn(CANCHA, HOY.plusDays(1), HOY.plusDays(3), OCHO_A_DIECIOCHO)).thenReturn(List.of());

        BloqueoResponse respuesta = servicio.bloquear(CANCHA, pedido(HOY.plusDays(1), HOY.plusDays(3)), ACTOR);

        assertThat(respuesta.dias()).isEqualTo(3);
        assertThat(respuesta.motivo()).isEqualTo("Cambio de superficie");
        ArgumentCaptor<List<Disponibilidad>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(disponibilidadRepository).saveAll(guardadas.capture());
        assertThat(guardadas.getValue()).extracting(Disponibilidad::getFecha)
                .containsExactly(HOY.plusDays(1), HOY.plusDays(2), HOY.plusDays(3));
        assertThat(guardadas.getValue()).allSatisfy(fila -> {
            assertThat(fila.getEstado()).isEqualTo(EstadoDisponibilidad.MANTENIMIENTO);
            assertThat(fila.getMotivoBloqueo()).isEqualTo("Cambio de superficie");
            assertThat(fila.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
            assertThat(fila.getHoraFin()).isEqualTo(LocalTime.of(18, 0));
            assertThat(fila.getCancha()).isSameAs(cancha);
        });
    }

    @Test
    void bloquear_hoyMismo_sePermite() {
        canchaExiste();

        BloqueoResponse respuesta = servicio.bloquear(CANCHA, pedido(HOY, HOY), ACTOR);

        assertThat(respuesta.dias()).isEqualTo(1);
    }

    @Test
    void bloquear_conReservasVigentes_409ConSusIdsYNoGuarda_A1() {
        canchaExiste();
        when(reservasDelRango.idsVigentesEn(CANCHA, HOY.plusDays(1), HOY.plusDays(1), OCHO_A_DIECIOCHO)).thenReturn(List.of(3L, 5L));

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido(HOY.plusDays(1), HOY.plusDays(1)), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("2 reserva(s)")
                .hasMessageContaining("3, 5")
                .extracting("codigo").isEqualTo("BLOQUEO_CON_RESERVAS");
        verify(disponibilidadRepository, never()).saveAll(anyList());
    }

    @Test
    void bloquear_sobreOtroBloqueo_409() {
        canchaExiste();
        Disponibilidad existente = new Disponibilidad();
        existente.setHoraInicio(LocalTime.of(10, 0));
        existente.setHoraFin(LocalTime.of(12, 0));
        when(disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(CANCHA, EstadoDisponibilidad.MANTENIMIENTO, HOY, HOY))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido(HOY, HOY), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("BLOQUEO_SUPERPUESTO");
        verify(disponibilidadRepository, never()).saveAll(anyList());
        verify(reservasDelRango, never()).idsVigentesEn(any(), any(), any(), any());
    }

    @Test
    void bloquear_otroBloqueoEnOtraHora_noChoca() {
        canchaExiste();
        Disponibilidad existente = new Disponibilidad();
        existente.setHoraInicio(LocalTime.of(18, 0));
        existente.setHoraFin(LocalTime.of(20, 0));
        when(disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(CANCHA, EstadoDisponibilidad.MANTENIMIENTO, HOY, HOY))
                .thenReturn(List.of(existente));

        assertThat(servicio.bloquear(CANCHA, pedido(HOY, HOY), ACTOR).dias()).isEqualTo(1);
    }

    @Test
    void bloquear_conFechaPasada_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido(HOY.minusDays(1), HOY), ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("pasada")
                .extracting("codigo").isEqualTo("BLOQUEO_INVALIDO");
    }

    @Test
    void bloquear_conFechaFinAnteriorALaDeInicio_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido(HOY.plusDays(3), HOY.plusDays(1)), ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void bloquear_masDeNoventaDias_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido(HOY, HOY.plusDays(90)), ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("90");
        verify(disponibilidadRepository, never()).saveAll(anyList());
    }

    @Test
    void bloquear_exactamenteNoventaDias_sePermite() {
        canchaExiste();

        assertThat(servicio.bloquear(CANCHA, pedido(HOY, HOY.plusDays(89)), ACTOR).dias()).isEqualTo(90);
    }

    @Test
    void bloquear_conHorasConMinutos_400() {
        canchaExiste();
        BloqueoRequest pedido = new BloqueoRequest(HOY, HOY, LocalTime.of(8, 15), LocalTime.of(18, 0), "Riego");

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido, ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("BLOQUEO_INVALIDO");
    }

    @Test
    void bloquear_canchaInexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.bloquear(99L, pedido(HOY, HOY), ACTOR)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void bloquear_canchaDeshabilitada_422() {
        cancha.setActiva(false);
        canchaExiste();

        assertThatThrownBy(() -> servicio.bloquear(CANCHA, pedido(HOY, HOY), ACTOR))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CANCHA_INACTIVA");
    }

    private Disponibilidad bloqueoGuardado(Long id, LocalDate fecha, int desde, int hasta) {
        Disponibilidad bloqueo = new Disponibilidad();
        bloqueo.setId(id);
        bloqueo.setFecha(fecha);
        bloqueo.setHoraInicio(LocalTime.of(desde, 0));
        bloqueo.setHoraFin(LocalTime.of(hasta, 0));
        bloqueo.setMotivoBloqueo("Riego");
        return bloqueo;
    }

    @Test
    void listar_devuelveLosDiasBloqueadosOrdenadosPorFechaYHora() {
        canchaExiste();
        when(disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(CANCHA, EstadoDisponibilidad.MANTENIMIENTO, HOY, HOY.plusDays(5)))
                .thenReturn(List.of(bloqueoGuardado(2L, HOY.plusDays(2), 8, 10), bloqueoGuardado(1L, HOY.plusDays(1), 14, 16)));

        List<BloqueoDelDiaResponse> bloqueos = servicio.listar(CANCHA, HOY, HOY.plusDays(5));

        assertThat(bloqueos).extracting(BloqueoDelDiaResponse::id).containsExactly(1L, 2L);
        assertThat(bloqueos.get(0).motivo()).isEqualTo("Riego");
    }

    @Test
    void listar_conRangoInvalidoOMayorA90Dias_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.listar(CANCHA, HOY.plusDays(2), HOY)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.listar(CANCHA, HOY, HOY.plusDays(90)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RANGO_DE_CONSULTA_INVALIDO");
    }

    @Test
    void desbloquear_borraElBloqueoDeEsaCancha() {
        canchaExiste();
        Disponibilidad bloqueo = bloqueoGuardado(7L, HOY, 8, 18);
        when(disponibilidadRepository.findByIdAndCanchaIdAndEstado(7L, CANCHA, EstadoDisponibilidad.MANTENIMIENTO)).thenReturn(Optional.of(bloqueo));

        servicio.desbloquear(CANCHA, 7L, ACTOR);

        verify(disponibilidadRepository).delete(bloqueo);
    }

    @Test
    void desbloquear_unBloqueoDeOtraCanchaOInexistente_404YNoBorra() {
        canchaExiste();
        when(disponibilidadRepository.findByIdAndCanchaIdAndEstado(7L, CANCHA, EstadoDisponibilidad.MANTENIMIENTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desbloquear(CANCHA, 7L, ACTOR))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("BLOQUEO_NO_ENCONTRADO");
        verify(disponibilidadRepository, never()).delete(any());
    }
}
