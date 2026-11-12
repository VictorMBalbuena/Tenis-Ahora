package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.cancha.service.FranjaDelDia;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.tenisahora.modules.actividad.DatosDePrueba.clase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadesDelRangoImplTest {

    private static final long CANCHA = 1L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 4);
    private static final List<EstadoActividad> OCUPAN = EstadoActividad.queOcupanCancha();

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ActividadDiaRepository diasRepository;

    private ActividadesDelRangoImpl impl;

    @BeforeEach
    void setUp() {
        impl = new ActividadesDelRangoImpl(actividadRepository, diasRepository);
    }

    @Test
    void ocupadasEn_devuelveUnaFranjaPorCadaFechaQueCaeEnUnDiaDeDictado() {
        Clase clase = clase(1L);
        clase.setFechaInicio(LUNES);
        clase.setFechaFin(MIERCOLES);
        when(actividadRepository.vigentesEnRango(CANCHA, LUNES, MIERCOLES, OCUPAN)).thenReturn(List.of(clase));
        when(diasRepository.findByActividadId(1L)).thenReturn(List.of(
                new ActividadDia(clase, DayOfWeek.MONDAY), new ActividadDia(clase, DayOfWeek.WEDNESDAY)));

        List<FranjaDelDia> ocupadas = impl.ocupadasEn(CANCHA, LUNES, MIERCOLES);

        assertThat(ocupadas).containsExactlyInAnyOrder(
                new FranjaDelDia(LUNES, LocalTime.of(18, 0), LocalTime.of(19, 0)),
                new FranjaDelDia(MIERCOLES, LocalTime.of(18, 0), LocalTime.of(19, 0)));
    }

    @Test
    void ocupadasEn_excluyeLasCanceladas_porqueElRepositorioYaFiltraPorEstado() {
        when(actividadRepository.vigentesEnRango(CANCHA, LUNES, MIERCOLES, OCUPAN)).thenReturn(List.of());

        assertThat(impl.ocupadasEn(CANCHA, LUNES, MIERCOLES)).isEmpty();
    }

    @Test
    void idsVigentesEnElDia_devuelveLosIdsQueDevuelveElRepositorio() {
        FranjaHoraria franja = new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(19, 0));
        when(actividadRepository.idsVigentesEnElDia(CANCHA, LUNES, DayOfWeek.MONDAY, franja.desde(), franja.hasta(), OCUPAN))
                .thenReturn(List.of(5L));

        assertThat(impl.idsVigentesEnElDia(CANCHA, LUNES, DayOfWeek.MONDAY, franja)).containsExactly(5L);
    }
}
