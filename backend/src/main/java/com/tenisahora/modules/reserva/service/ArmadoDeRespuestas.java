package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.modules.reserva.mapper.ReservaMapper;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaJugadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Convierte reservas en respuestas trayendo los jugadores de todas juntas, sin una consulta por cada una. */
@Component
@RequiredArgsConstructor
public class ArmadoDeRespuestas {

    private final ReservaJugadorRepository jugadorRepository;
    private final ReservaInsumoRepository insumoRepository;
    private final ReservaMapper reservaMapper;

    public ReservaResponse deReserva(Reserva reserva) {
        return deReservas(List.of(reserva)).get(0);
    }

    public List<ReservaResponse> deReservas(List<Reserva> reservas) {
        List<Long> ids = reservas.stream().map(Reserva::getId).toList();
        Map<Long, List<ReservaJugador>> jugadores = ids.isEmpty() ? Map.of()
                : jugadorRepository.findByReservaIdInOrderByReservaIdAscEsTitularDescIdAsc(ids).stream()
                        .collect(Collectors.groupingBy(jugador -> jugador.getReserva().getId()));
        Map<Long, List<ReservaInsumo>> insumos = ids.isEmpty() ? Map.of()
                : insumoRepository.findByReservaIdIn(ids).stream().collect(Collectors.groupingBy(asignado -> asignado.getReserva().getId()));
        return reservas.stream()
                .map(reserva -> reservaMapper.aRespuesta(reserva, jugadores.getOrDefault(reserva.getId(), List.of()),
                        insumos.getOrDefault(reserva.getId(), List.of())))
                .toList();
    }
}
