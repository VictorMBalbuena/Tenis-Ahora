package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AsignacionDeInstructorRequest;
import com.tenisahora.modules.actividad.dto.InstructorDisponibleResponse;
import com.tenisahora.shared.dto.Actor;

import java.util.List;

/** CU44: asignar un profesor o un entrenador a una actividad particular. */
public interface AsignacionDeInstructorAActividad {

    /** Profesores y entrenadores validados y sin otra actividad que se pise con el horario de esta. */
    List<InstructorDisponibleResponse> disponibles(Long actividadId);

    /** Vuelve a validar la certificación (nunca se confía en que el cliente eligió de la lista) y asigna, limpiando el otro rol. */
    void asignar(Long actividadId, AsignacionDeInstructorRequest request, Actor actor);
}
