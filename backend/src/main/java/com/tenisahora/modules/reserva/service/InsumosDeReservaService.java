package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.AsignacionDeInsumosRequest;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface InsumosDeReservaService {

    /** CU13: reemplaza los insumos de la reserva. Solo mientras no empezó: el titular o el personal. */
    ReservaResponse asignar(Long reservaId, AsignacionDeInsumosRequest request, Actor actor);

    /** CU13 incluye CU11: los insumos activos con las unidades que se pueden asignar a un turno. */
    List<InsumoDisponibleResponse> disponibles(LocalDate fecha, LocalTime horaInicio, int duracionHoras);
}
