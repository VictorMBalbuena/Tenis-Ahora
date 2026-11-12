package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.MiEntrenamientoResponse;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;

import java.util.List;

/** CU83: lo que un entrenador puede ver de sus propios entrenamientos. Espejo de ClasesDelProfesor. */
public interface EntrenamientosDelEntrenador {

    /** Requiere que el entrenador tenga la certificación validada. */
    PageResponse<MiEntrenamientoResponse> misEntrenamientos(Actor actor, int page, int size);

    /** Los inscriptos de un entrenamiento propio; uno ajeno responde 404. */
    List<AlumnoDeClaseResponse> inscriptos(Long entrenamientoId, Actor actor);
}
