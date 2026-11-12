package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Component;

/**
 * Profesor y entrenador son las dos columnas nullable de {@link Actividad} (DER); quién exige cuál lo valida acá, no
 * el esquema: una clase grupal exige profesor, un entrenamiento grupal exige entrenador, y una particular (CU44)
 * acepta cualquiera de los dos.
 */
@Component
public class ValidacionDeInstructorPorModalidad {

    public void asegurarValida(Actividad actividad) {
        boolean esClase = actividad instanceof Clase;
        boolean tieneProfesor = actividad.getProfesor() != null;
        boolean tieneEntrenador = actividad.getEntrenador() != null;

        if (actividad.getModalidad() == Modalidad.PARTICULAR) {
            if (!tieneProfesor && !tieneEntrenador) {
                throw sinInstructor("Una actividad particular necesita un profesor o un entrenador asignado");
            }
            return;
        }
        if (esClase && !tieneProfesor) {
            throw sinInstructor("Una clase grupal necesita un profesor asignado");
        }
        if (!esClase && !tieneEntrenador) {
            throw sinInstructor("Un entrenamiento grupal necesita un entrenador asignado");
        }
    }

    private ReglaDeNegocioException sinInstructor(String mensaje) {
        return new ReglaDeNegocioException(CodigoError.INSTRUCTOR_REQUERIDO, mensaje);
    }
}
