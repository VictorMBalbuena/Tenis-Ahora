package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.EntrenamientoRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.actividad.DatosDePrueba.entrenador;
import static com.tenisahora.modules.actividad.DatosDePrueba.entrenamiento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Espejo de ClasesDelProfesorImplTest. */
@ExtendWith(MockitoExtension.class)
class EntrenamientosDelEntrenadorImplTest {

    private static final long ENTRENADOR = 5L;
    private static final Actor ENTRENADOR_ACTOR = new Actor(ENTRENADOR, Rol.ENTRENADOR);
    private static final Actor PROFESOR = new Actor(4L, Rol.PROFESOR);

    @Mock
    private EntrenamientoRepository entrenamientoRepository;
    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private EntrenadorRepository entrenadorRepository;
    @Mock
    private ArmadoDeRespuestasDeEntrenamiento respuestas;
    @Mock
    private ClaseMapper claseMapper;
    @Mock
    private AccesoAActividades acceso;

    private EntrenamientosDelEntrenadorImpl impl;

    @BeforeEach
    void setUp() {
        impl = new EntrenamientosDelEntrenadorImpl(entrenamientoRepository, inscripcionRepository, entrenadorRepository, respuestas, claseMapper, acceso);
    }

    @Test
    void misEntrenamientos_devuelveSoloLosPropios() {
        when(entrenadorRepository.findById(ENTRENADOR)).thenReturn(Optional.of(entrenador(ENTRENADOR, true)));
        Entrenamiento entrenamiento = entrenamiento(10L);
        when(entrenamientoRepository.findByEntrenadorId(eq(ENTRENADOR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entrenamiento), PageRequest.of(0, 20), 1));

        PageResponse<?> pagina = impl.misEntrenamientos(ENTRENADOR_ACTOR, 0, 20);

        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    @Test
    void misEntrenamientos_sinEntrenamientos_paginaVacia_A1() {
        when(entrenadorRepository.findById(ENTRENADOR)).thenReturn(Optional.of(entrenador(ENTRENADOR, true)));
        when(entrenamientoRepository.findByEntrenadorId(eq(ENTRENADOR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(impl.misEntrenamientos(ENTRENADOR_ACTOR, 0, 20).contenido()).isEmpty();
    }

    @Test
    void misEntrenamientos_entrenadorSinCertificacionValidada_422() {
        when(entrenadorRepository.findById(ENTRENADOR)).thenReturn(Optional.of(entrenador(ENTRENADOR, false)));

        assertThatThrownBy(() -> impl.misEntrenamientos(ENTRENADOR_ACTOR, 0, 20))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_NO_VALIDADO");
    }

    @Test
    void inscriptos_deUnEntrenamientoAjeno_404() {
        when(acceso.paraOperar(10L, Entrenamiento.class, CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, PROFESOR)).thenThrow(
                new RecursoNoEncontradoException(CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, "Entrenamiento no encontrado"));

        assertThatThrownBy(() -> impl.inscriptos(10L, PROFESOR)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void inscriptos_deUnEntrenamientoPropio_losDevuelve() {
        Entrenamiento entrenamiento = entrenamiento(10L);
        when(acceso.paraOperar(10L, Entrenamiento.class, CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, ENTRENADOR_ACTOR)).thenReturn(entrenamiento);
        when(inscripcionRepository.deLaActividad(10L)).thenReturn(List.of());

        assertThat(impl.inscriptos(10L, ENTRENADOR_ACTOR)).isEmpty();
    }

    @Test
    void misEntrenamientos_comoProfesor_noAplica() {
        when(entrenadorRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> impl.misEntrenamientos(PROFESOR, 0, 20))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
