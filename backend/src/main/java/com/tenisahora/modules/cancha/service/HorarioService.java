package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.HorarioRequest;
import com.tenisahora.modules.cancha.dto.HorarioResponse;

import java.util.List;

public interface HorarioService {

    /** CU090: registra la misma franja para cada día indicado, o ninguna si alguna se pisa con lo ya registrado. */
    List<HorarioResponse> registrar(Long canchaId, HorarioRequest request, Long actorId);

    List<HorarioResponse> listar(Long canchaId);

    /** Quita un horario semanal cargado por error. Las reservas ya hechas no se tocan. */
    void eliminar(Long canchaId, Long horarioId, Long actorId);
}
