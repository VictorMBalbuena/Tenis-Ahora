package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.EstadoFranja;
import com.tenisahora.modules.cancha.dto.FranjaResponse;
import com.tenisahora.modules.cancha.dto.GrillaResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.Disponibilidad;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceImplTest {

    private static final long CANCHA = 1L;
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);

    @Mock
    private HorarioSemanalRepository horarioRepository;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Mock
    private ReservasDelRango reservasDelRango;
    @Mock
    private ActividadesDelRango actividadesDelRango;
    @Mock
    private CanchaRepository canchaRepository;

    private DisponibilidadServiceImpl servicio;
    private Cancha cancha;

    @BeforeEach
    void setUp() {
        servicio = new DisponibilidadServiceImpl(horarioRepository, disponibilidadRepository, reservasDelRango, actividadesDelRango,
                new CanchaPorId(canchaRepository));
        cancha = new Cancha();
        cancha.setId(CANCHA);
        cancha.setActiva(true);
    }

    private void canchaExiste() {
        when(canchaRepository.findById(CANCHA)).thenReturn(Optional.of(cancha));
    }

    @Test
    void consultar_cruzaHorariosBloqueosYReservas() {
        canchaExiste();
        HorarioSemanal horario = new HorarioSemanal();
        horario.setDiaSemana(DayOfWeek.MONDAY);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(11, 0));
        Disponibilidad bloqueo = new Disponibilidad();
        bloqueo.setFecha(LUNES);
        bloqueo.setHoraInicio(LocalTime.of(8, 0));
        bloqueo.setHoraFin(LocalTime.of(9, 0));
        when(horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(CANCHA)).thenReturn(List.of(horario));
        when(disponibilidadRepository.findByCanchaIdAndEstadoAndFechaBetween(CANCHA, EstadoDisponibilidad.MANTENIMIENTO, LUNES, LUNES))
                .thenReturn(List.of(bloqueo));
        when(reservasDelRango.ocupadasEn(CANCHA, LUNES, LUNES))
                .thenReturn(List.of(new FranjaDelDia(LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0))));

        GrillaResponse grilla = servicio.consultar(CANCHA, LUNES, LUNES);

        assertThat(grilla.canchaId()).isEqualTo(CANCHA);
        assertThat(grilla.franjas()).extracting(FranjaResponse::estado)
                .containsExactly(EstadoFranja.BLOQUEADA, EstadoFranja.OCUPADA, EstadoFranja.LIBRE);
    }

    @Test
    void consultar_sinHorarios_devuelveGrillaVacia_A1() {
        canchaExiste();
        when(horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(CANCHA)).thenReturn(List.of());

        assertThat(servicio.consultar(CANCHA, LUNES, LUNES.plusDays(2)).franjas()).isEmpty();
    }

    @Test
    void consultar_canchaDeshabilitada_grillaVaciaSinConsultarNada() {
        cancha.setActiva(false);
        canchaExiste();

        assertThat(servicio.consultar(CANCHA, LUNES, LUNES).franjas()).isEmpty();
        verify(horarioRepository, never()).findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(any());
        verify(reservasDelRango, never()).ocupadasEn(any(), any(), any());
    }

    @Test
    void consultar_conFinAnteriorAlInicio_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.consultar(CANCHA, LUNES, LUNES.minusDays(1)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RANGO_DE_CONSULTA_INVALIDO");
    }

    @Test
    void consultar_masDe31Dias_400() {
        canchaExiste();

        assertThatThrownBy(() -> servicio.consultar(CANCHA, LUNES, LUNES.plusDays(31)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("31");
    }

    @Test
    void consultar_exactamente31Dias_sePermite() {
        canchaExiste();

        assertThat(servicio.consultar(CANCHA, LUNES, LUNES.plusDays(30)).franjas()).isEmpty();
    }

    @Test
    void consultar_canchaInexistente_404() {
        when(canchaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(99L, LUNES, LUNES)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
