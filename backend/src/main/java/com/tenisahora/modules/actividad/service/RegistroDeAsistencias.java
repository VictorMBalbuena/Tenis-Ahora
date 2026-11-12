package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.AsistenciaResponse;
import com.tenisahora.modules.actividad.dto.MarcaDeAsistenciaRequest;
import com.tenisahora.shared.dto.Actor;

import java.time.LocalDate;
import java.util.List;

/** CU42 y CU43: un solo flujo para clases y entrenamientos, con distinto actor (profesor o entrenador). */
public interface RegistroDeAsistencias {

    /** Marca en lote presente o ausente a cada alumno. Volver a registrar la misma fecha corrige, no duplica. */
    List<AsistenciaResponse> registrar(Long actividadId, LocalDate fecha, List<MarcaDeAsistenciaRequest> marcas, Actor actor);

    /** Los inscriptos de la actividad con su marca de esa fecha (ausente si todavía no se registró). */
    List<AsistenciaResponse> delDia(Long actividadId, LocalDate fecha, Actor actor);
}
