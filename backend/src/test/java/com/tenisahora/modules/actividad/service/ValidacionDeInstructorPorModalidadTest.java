package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import static com.tenisahora.modules.actividad.DatosDePrueba.entrenador;
import static com.tenisahora.modules.actividad.DatosDePrueba.profesor;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionDeInstructorPorModalidadTest {

    private final ValidacionDeInstructorPorModalidad validacion = new ValidacionDeInstructorPorModalidad();

    @Test
    void claseGrupal_conProfesor_esValida() {
        Clase clase = new Clase();
        clase.setModalidad(Modalidad.GRUPAL);
        clase.setProfesor(profesor(4L, true));

        assertThatCode(() -> validacion.asegurarValida(clase)).doesNotThrowAnyException();
    }

    @Test
    void claseGrupal_sinProfesor_422_INSTRUCTOR_REQUERIDO() {
        Clase clase = new Clase();
        clase.setModalidad(Modalidad.GRUPAL);

        assertThatThrownBy(() -> validacion.asegurarValida(clase))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_REQUERIDO");
    }

    @Test
    void entrenamientoGrupal_conEntrenador_esValido() {
        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setModalidad(Modalidad.GRUPAL);
        entrenamiento.setEntrenador(entrenador(5L, true));

        assertThatCode(() -> validacion.asegurarValida(entrenamiento)).doesNotThrowAnyException();
    }

    @Test
    void entrenamientoGrupal_sinEntrenador_422_INSTRUCTOR_REQUERIDO() {
        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setModalidad(Modalidad.GRUPAL);

        assertThatThrownBy(() -> validacion.asegurarValida(entrenamiento))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_REQUERIDO");
    }

    @Test
    void particular_aceptaCualquieraDeLosDos() {
        Clase conProfesor = new Clase();
        conProfesor.setModalidad(Modalidad.PARTICULAR);
        conProfesor.setProfesor(profesor(4L, true));

        Entrenamiento conEntrenador = new Entrenamiento();
        conEntrenador.setModalidad(Modalidad.PARTICULAR);
        conEntrenador.setEntrenador(entrenador(5L, true));

        assertThatCode(() -> validacion.asegurarValida(conProfesor)).doesNotThrowAnyException();
        assertThatCode(() -> validacion.asegurarValida(conEntrenador)).doesNotThrowAnyException();
    }

    @Test
    void particular_sinNingunoDeLosDos_422_INSTRUCTOR_REQUERIDO() {
        Clase particular = new Clase();
        particular.setModalidad(Modalidad.PARTICULAR);

        assertThatThrownBy(() -> validacion.asegurarValida(particular))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSTRUCTOR_REQUERIDO");
    }
}
