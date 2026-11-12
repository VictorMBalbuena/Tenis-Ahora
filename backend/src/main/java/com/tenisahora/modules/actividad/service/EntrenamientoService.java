package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.dto.EntrenamientoRequest;
import com.tenisahora.modules.actividad.dto.EntrenamientoResponse;
import com.tenisahora.modules.actividad.dto.EntrenamientoUpdateRequest;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PageResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;

public interface EntrenamientoService {

    /** CU37: programa el entrenamiento validando entrenador, cancha, agenda y cupo. */
    EntrenamientoResponse programar(EntrenamientoRequest request, Actor actor);

    /** CU39: búsqueda con filtros opcionales, todos combinables. */
    PageResponse<EntrenamientoResponse> buscar(Long canchaId, Long entrenadorId, LocalDate fecha, DayOfWeek dia,
                                               EstadoActividad estado, int page, int size);

    /** CU39: consulta puntual. */
    EntrenamientoResponse consultar(Long id);

    /** CU38: modificación parcial, revalida la agenda y el cupo. */
    EntrenamientoResponse modificar(Long id, EntrenamientoUpdateRequest request, Actor actor);

    /** CU40: solo si no tiene inscripciones activas. */
    void eliminar(Long id);

    /** CU41: ficha en PDF. */
    byte[] imprimir(Long id);
}
