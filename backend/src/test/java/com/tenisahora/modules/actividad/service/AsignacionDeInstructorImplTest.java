package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AsignacionDeInstructorRequest;
import com.tenisahora.modules.actividad.dto.InstructorDisponibleResponse;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.actividad.DatosDePrueba.entrenador;
import static com.tenisahora.modules.actividad.DatosDePrueba.entrenamiento;
import static com.tenisahora.modules.actividad.DatosDePrueba.profesor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionDeInstructorImplTest {

    private static final long ACTIVIDAD = 10L;
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ActividadDiaRepository diasRepository;
    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private EntrenadorRepository entrenadorRepository;

    private AsignacionDeInstructorAActividadImpl impl;
    private Entrenamiento particular;

    @BeforeEach
    void setUp() {
        impl = new AsignacionDeInstructorAActividadImpl(actividadRepository, diasRepository, profesorRepository, entrenadorRepository);
        particular = entrenamiento(ACTIVIDAD);
        particular.setModalidad(Modalidad.PARTICULAR);
    }

    private void actividadParticularExiste() {
        when(actividadRepository.findById(ACTIVIDAD)).thenReturn(Optional.of(particular));
        when(diasRepository.findByActividadId(ACTIVIDAD)).thenReturn(List.of(new ActividadDia(particular, DayOfWeek.MONDAY)));
    }

    @Test
    void disponibles_listaSoloInstructoresValidadosYLibres() {
        actividadParticularExiste();
        when(profesorRepository.findAll()).thenReturn(List.of(profesor(4L, true), profesor(6L, false)));
        when(entrenadorRepository.findAll()).thenReturn(List.of(entrenador(5L, true)));
        when(actividadRepository.profesorConSuperposicion(eq(4L), any(), any(), any(), any(), any(), any(), eq(ACTIVIDAD))).thenReturn(false);
        when(actividadRepository.entrenadorConSuperposicion(eq(5L), any(), any(), any(), any(), any(), any(), eq(ACTIVIDAD))).thenReturn(false);

        List<InstructorDisponibleResponse> disponibles = impl.disponibles(ACTIVIDAD);

        assertThat(disponibles).extracting(InstructorDisponibleResponse::id).containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    void disponibles_entrenadorOcupadoEnEseHorario_noAparece() {
        actividadParticularExiste();
        when(profesorRepository.findAll()).thenReturn(List.of());
        when(entrenadorRepository.findAll()).thenReturn(List.of(entrenador(5L, true)));
        when(actividadRepository.entrenadorConSuperposicion(eq(5L), any(), any(), any(), any(), any(), any(), eq(ACTIVIDAD))).thenReturn(true);

        assertThat(impl.disponibles(ACTIVIDAD)).isEmpty();
    }

    @Test
    void disponibles_sobreUnaGrupal_422_MODALIDAD_INVALIDA() {
        particular.setModalidad(Modalidad.GRUPAL);
        when(actividadRepository.findById(ACTIVIDAD)).thenReturn(Optional.of(particular));

        assertThatThrownBy(() -> impl.disponibles(ACTIVIDAD))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("MODALIDAD_INVALIDA");
    }

    @Test
    void asignar_unEntrenadorValidadoAUnaParticular_funciona() {
        when(actividadRepository.findById(ACTIVIDAD)).thenReturn(Optional.of(particular));
        when(entrenadorRepository.findById(5L)).thenReturn(Optional.of(entrenador(5L, true)));
        particular.setProfesor(profesor(4L, true));

        impl.asignar(ACTIVIDAD, new AsignacionDeInstructorRequest(Rol.ENTRENADOR, 5L), EMPLEADO);

        assertThat(particular.getEntrenador().getId()).isEqualTo(5L);
        assertThat(particular.getProfesor()).isNull();
    }

    @Test
    void asignar_sinCertificacionValidada_422_A1() {
        when(actividadRepository.findById(ACTIVIDAD)).thenReturn(Optional.of(particular));
        when(entrenadorRepository.findById(5L)).thenReturn(Optional.of(entrenador(5L, false)));

        assertThatThrownBy(() -> impl.asignar(ACTIVIDAD, new AsignacionDeInstructorRequest(Rol.ENTRENADOR, 5L), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
    }

    @Test
    void asignar_sobreUnaGrupal_422_MODALIDAD_INVALIDA() {
        particular.setModalidad(Modalidad.GRUPAL);
        when(actividadRepository.findById(ACTIVIDAD)).thenReturn(Optional.of(particular));

        assertThatThrownBy(() -> impl.asignar(ACTIVIDAD, new AsignacionDeInstructorRequest(Rol.ENTRENADOR, 5L), EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("MODALIDAD_INVALIDA");
    }
}
