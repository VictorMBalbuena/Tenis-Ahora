package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ClaseRepository;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;
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

import static com.tenisahora.modules.actividad.DatosDePrueba.clase;
import static com.tenisahora.modules.actividad.DatosDePrueba.profesor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClasesDelProfesorImplTest {

    private static final long PROFESOR = 4L;
    private static final Actor PROFESOR_ACTOR = new Actor(PROFESOR, Rol.PROFESOR);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private ArmadoDeRespuestasDeClase respuestas;
    @Mock
    private ClaseMapper claseMapper;
    @Mock
    private AccesoAActividades acceso;

    private ClasesDelProfesorImpl impl;

    @BeforeEach
    void setUp() {
        impl = new ClasesDelProfesorImpl(claseRepository, inscripcionRepository, profesorRepository, respuestas, claseMapper, acceso);
    }

    @Test
    void misClases_devuelveSoloLasPropias() {
        when(profesorRepository.findById(PROFESOR)).thenReturn(Optional.of(profesor(PROFESOR, true)));
        Clase clase = clase(10L);
        when(claseRepository.findByProfesorId(eq(PROFESOR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(clase), PageRequest.of(0, 20), 1));

        PageResponse<?> pagina = impl.misClases(PROFESOR_ACTOR, 0, 20);

        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    @Test
    void misClases_sinClases_paginaVacia_A1() {
        when(profesorRepository.findById(PROFESOR)).thenReturn(Optional.of(profesor(PROFESOR, true)));
        when(claseRepository.findByProfesorId(eq(PROFESOR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(impl.misClases(PROFESOR_ACTOR, 0, 20).contenido()).isEmpty();
    }

    @Test
    void misClases_profesorSinCertificacionValidada_422() {
        when(profesorRepository.findById(PROFESOR)).thenReturn(Optional.of(profesor(PROFESOR, false)));

        assertThatThrownBy(() -> impl.misClases(PROFESOR_ACTOR, 0, 20))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("PROFESOR_NO_VALIDADO");
    }

    @Test
    void inscriptos_deUnaClaseAjena_404() {
        when(acceso.paraOperar(10L, Clase.class, com.tenisahora.shared.exception.CodigoError.CLASE_NO_ENCONTRADA, PROFESOR_ACTOR)).thenThrow(
                new RecursoNoEncontradoException(com.tenisahora.shared.exception.CodigoError.CLASE_NO_ENCONTRADA, "Clase no encontrada"));

        assertThatThrownBy(() -> impl.inscriptos(10L, PROFESOR_ACTOR)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void inscriptos_deUnaClasePropia_losDevuelve() {
        Clase clase = clase(10L);
        when(acceso.paraOperar(10L, Clase.class, com.tenisahora.shared.exception.CodigoError.CLASE_NO_ENCONTRADA, PROFESOR_ACTOR)).thenReturn(clase);
        when(inscripcionRepository.deLaActividad(10L)).thenReturn(List.of());

        assertThat(impl.inscriptos(10L, PROFESOR_ACTOR)).isEmpty();
    }

    @Test
    void inscriptos_comoEmpleado_tambienFunciona() {
        Clase clase = clase(10L);
        when(acceso.paraOperar(10L, Clase.class, com.tenisahora.shared.exception.CodigoError.CLASE_NO_ENCONTRADA, EMPLEADO)).thenReturn(clase);
        when(inscripcionRepository.deLaActividad(10L)).thenReturn(List.of());

        assertThat(impl.inscriptos(10L, EMPLEADO)).isEmpty();
    }
}
