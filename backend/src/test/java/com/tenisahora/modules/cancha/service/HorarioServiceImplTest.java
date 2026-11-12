package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.HorarioRequest;
import com.tenisahora.modules.cancha.dto.HorarioResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import com.tenisahora.modules.cancha.mapper.HorarioMapper;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioServiceImplTest {

    private static final long CANCHA = 1L;
    private static final long ACTOR = 2L;
    private static final java.time.Clock RELOJ = java.time.Clock.fixed(java.time.Instant.parse("2026-10-19T10:00:00Z"), java.time.ZoneOffset.UTC);
    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DOCE = LocalTime.of(12, 0);

    @Mock
    private HorarioSemanalRepository horarioRepository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private ReservasDelRango reservasDelRango;

    private HorarioServiceImpl servicio;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        servicio = new HorarioServiceImpl(horarioRepository, new CanchaPorId(canchaRepository), new HorarioMapper(), reservasDelRango, RELOJ);
        cancha = new Cancha();
        cancha.setId(CANCHA);
        cancha.setActiva(true);
    }

    private void canchaExiste() {
        when(canchaRepository.findById(CANCHA)).thenReturn(Optional.of(cancha));
    }

    @Test
    void registrar_guardaUnHorarioPorCadaDiaOrdenadoPorDia() {
        canchaExiste();
        when(horarioRepository.haySolapamiento(eq(CANCHA), any(), any(), any())).thenReturn(false);
        when(horarioRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HorarioResponse> respuesta = servicio.registrar(CANCHA,
                new HorarioRequest(Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY), OCHO, DOCE), ACTOR);

        assertThat(respuesta).extracting(HorarioResponse::dia).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        assertThat(respuesta).extracting(HorarioResponse::desde).containsOnly(OCHO);
        assertThat(respuesta).extracting(HorarioResponse::hasta).containsOnly(DOCE);
        ArgumentCaptor<List<HorarioSemanal>> guardados = ArgumentCaptor.forClass(List.class);
        verify(horarioRepository).saveAll(guardados.capture());
        assertThat(guardados.getValue()).allSatisfy(horario -> assertThat(horario.getCancha()).isSameAs(cancha));
    }

    @Test
    void registrar_siUnDiaSePisa_409YNoGuardaNingunoTodoONada() {
        canchaExiste();
        when(horarioRepository.haySolapamiento(CANCHA, DayOfWeek.MONDAY, OCHO, DOCE)).thenReturn(false);
        when(horarioRepository.haySolapamiento(CANCHA, DayOfWeek.TUESDAY, OCHO, DOCE)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(CANCHA,
                new HorarioRequest(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), OCHO, DOCE), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("TUESDAY")
                .extracting("codigo").isEqualTo("HORARIO_SUPERPUESTO");
        verify(horarioRepository, never()).saveAll(anyList());
    }

    @Test
    void registrar_conHorasConMinutos_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.registrar(CANCHA,
                new HorarioRequest(Set.of(DayOfWeek.MONDAY), LocalTime.of(8, 30), DOCE), ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("HORARIO_INVALIDO");
        verify(horarioRepository, never()).saveAll(anyList());
    }

    @Test
    void registrar_conInicioPosteriorAlFin_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.registrar(CANCHA,
                new HorarioRequest(Set.of(DayOfWeek.MONDAY), DOCE, OCHO), ACTOR))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void registrar_enCanchaInexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(99L, new HorarioRequest(Set.of(DayOfWeek.MONDAY), OCHO, DOCE), ACTOR))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registrar_enCanchaDeshabilitada_422() {
        cancha.setActiva(false);
        canchaExiste();

        assertThatThrownBy(() -> servicio.registrar(CANCHA, new HorarioRequest(Set.of(DayOfWeek.MONDAY), OCHO, DOCE), ACTOR))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CANCHA_INACTIVA");
    }

    @Test
    void listar_devuelveLosHorariosDeLaCancha() {
        canchaExiste();
        HorarioSemanal horario = new HorarioSemanal();
        horario.setId(5L);
        horario.setDiaSemana(DayOfWeek.FRIDAY);
        horario.setHoraInicio(OCHO);
        horario.setHoraFin(DOCE);
        when(horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(CANCHA)).thenReturn(List.of(horario));

        assertThat(servicio.listar(CANCHA)).containsExactly(new HorarioResponse(5L, DayOfWeek.FRIDAY, OCHO, DOCE));
    }

    @Test
    void listar_enCanchaInexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminar_borraElHorarioDeEsaCancha() {
        canchaExiste();
        HorarioSemanal horario = new HorarioSemanal();
        horario.setId(5L);
        when(horarioRepository.findByIdAndCanchaId(5L, CANCHA)).thenReturn(Optional.of(horario));

        horario.setDiaSemana(DayOfWeek.TUESDAY);
        horario.setHoraInicio(OCHO);
        horario.setHoraFin(DOCE);
        when(reservasDelRango.idsVigentesEnElDia(CANCHA, java.time.LocalDate.of(2026, 10, 19), DayOfWeek.TUESDAY, new FranjaHoraria(OCHO, DOCE)))
                .thenReturn(List.of());

        servicio.eliminar(CANCHA, 5L, ACTOR);

        verify(horarioRepository).delete(horario);
    }

    @Test
    void eliminar_unHorarioDeOtraCanchaOInexistente_404YNoBorra() {
        canchaExiste();
        when(horarioRepository.findByIdAndCanchaId(5L, CANCHA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(CANCHA, 5L, ACTOR))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("HORARIO_NO_ENCONTRADO");
        verify(horarioRepository, never()).delete(any());
    }

    @Test
    void eliminar_enCanchaInexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(99L, 5L, ACTOR)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminar_conReservasVigentes_409ConSusIdsYNoBorra() {
        canchaExiste();
        HorarioSemanal horario = new HorarioSemanal();
        horario.setId(5L);
        horario.setDiaSemana(DayOfWeek.TUESDAY);
        horario.setHoraInicio(OCHO);
        horario.setHoraFin(DOCE);
        when(horarioRepository.findByIdAndCanchaId(5L, CANCHA)).thenReturn(Optional.of(horario));
        when(reservasDelRango.idsVigentesEnElDia(CANCHA, java.time.LocalDate.of(2026, 10, 19), DayOfWeek.TUESDAY, new FranjaHoraria(OCHO, DOCE)))
                .thenReturn(List.of(3L, 8L));

        assertThatThrownBy(() -> servicio.eliminar(CANCHA, 5L, ACTOR))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("2 reserva(s)")
                .hasMessageContaining("3, 8")
                .hasMessageContaining("reprogramalas")
                .extracting("codigo").isEqualTo("HORARIO_CON_RESERVAS");
        verify(horarioRepository, never()).delete(any());
    }
}
