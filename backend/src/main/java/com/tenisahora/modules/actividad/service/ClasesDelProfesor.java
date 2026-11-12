package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.MiClaseResponse;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;

import java.util.List;

/** CU81: lo que un profesor puede ver de sus propias clases. */
public interface ClasesDelProfesor {

    /** Requiere que el profesor tenga la certificación validada. */
    PageResponse<MiClaseResponse> misClases(Actor actor, int page, int size);

    /** Los inscriptos de una clase propia; una ajena responde 404. */
    List<AlumnoDeClaseResponse> inscriptos(Long claseId, Actor actor);
}
