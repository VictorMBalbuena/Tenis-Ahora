package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Quién puede operar sobre una clase o un entrenamiento: el personal cualquiera, el profesor o el entrenador solo
 * los propios. Una actividad ajena (o de otro tipo) responde 404, no 403, así no se puede averiguar qué existe
 * probando números. Espejo de AccesoAReservas. La usan CU81, CU83 (ver inscriptos) y CU42/CU43 (registrar asistencia).
 */
@Component
@RequiredArgsConstructor
public class AccesoAActividades {

    private final ActividadRepository actividadRepository;

    public static boolean esPersonal(Actor actor) {
        return actor.rol() == Rol.EMPLEADO || actor.rol() == Rol.ADMINISTRADOR;
    }

    /** @param tipo el tipo esperado (Clase, Entrenamiento o Actividad si cualquiera de los dos sirve). */
    public <T extends Actividad> T paraOperar(Long actividadId, Class<T> tipo, CodigoError codigoNoEncontrada, Actor actor) {
        T actividad = actividadExistente(actividadId, tipo, codigoNoEncontrada);
        if (esPersonal(actor) || esPropia(actividad, actor)) {
            return actividad;
        }
        throw noEncontrada(codigoNoEncontrada);
    }

    private boolean esPropia(Actividad actividad, Actor actor) {
        return actividad.tieneInstructor() && actividad.getInstructor().getRol() == actor.rol()
                && actividad.getInstructor().getId().equals(actor.id());
    }

    private <T extends Actividad> T actividadExistente(Long actividadId, Class<T> tipo, CodigoError codigoNoEncontrada) {
        return actividadRepository.findById(actividadId)
                .filter(tipo::isInstance)
                .map(tipo::cast)
                .orElseThrow(() -> noEncontrada(codigoNoEncontrada));
    }

    private RecursoNoEncontradoException noEncontrada(CodigoError codigo) {
        return new RecursoNoEncontradoException(codigo, "No encontrada");
    }
}
