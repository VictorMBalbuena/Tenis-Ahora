package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.ClaseRequest;
import com.tenisahora.modules.actividad.dto.ClaseResponse;
import com.tenisahora.modules.actividad.dto.ClaseUpdateRequest;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;

public interface ClaseService {

    /** CU32: programa la clase validando profesor, cancha, agenda y cupo. */
    ClaseResponse programar(ClaseRequest request, Actor actor);

    /** CU34: búsqueda con filtros opcionales, todos combinables. */
    PageResponse<ClaseResponse> buscar(Long canchaId, Long profesorId, LocalDate fecha, DayOfWeek dia, NivelClase nivel,
                                       EstadoActividad estado, int page, int size);

    /** CU34: consulta puntual. */
    ClaseResponse consultar(Long id);

    /** CU33: modificación parcial, revalida la agenda y el cupo. */
    ClaseResponse modificar(Long id, ClaseUpdateRequest request, Actor actor);

    /** CU35: solo si no tiene inscripciones activas. */
    void eliminar(Long id);

    /** CU36: ficha en PDF. */
    byte[] imprimir(Long id);
}
