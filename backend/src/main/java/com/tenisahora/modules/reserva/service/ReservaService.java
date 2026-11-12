package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.ReservaRequest;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.dto.ReservaUpdateRequest;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.shared.dto.PageResponse;

import java.time.LocalDate;

public interface ReservaService {

    /** CU16: la reserva nace pendiente de pago y retiene el horario hasta que se pague la seña o venza. */
    ReservaResponse registrar(ReservaRequest request, Actor actor);

    /** Una reserva que el actor puede ver: el personal cualquiera, un socio solo las suyas. */
    ReservaResponse consultar(Long id, Actor actor);

    /** Consultar mis reservas: las del socio, próximas o pasadas. */
    PageResponse<ReservaResponse> misReservas(Boolean proximas, EstadoReserva estado, int page, int size, Actor actor);

    /** CU17: búsqueda del personal por documento del titular o número de reserva, con filtros opcionales. */
    PageResponse<ReservaResponse> buscar(String documento, String numero, EstadoReserva estado, LocalDate fecha,
                                         Long canchaId, int page, int size);

    /** CU17: cambia cancha, fecha u horario y recalcula el monto. */
    ReservaResponse modificar(Long id, ReservaUpdateRequest request, Actor actor);
}
