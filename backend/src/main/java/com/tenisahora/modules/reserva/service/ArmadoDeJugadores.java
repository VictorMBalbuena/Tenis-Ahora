package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.modules.reserva.dto.JugadorRequest;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Arma los jugadores de una reserva: el titular y los acompañantes, registrados (activos) o invitados con nombre y apellido (RD07). */
@Component
@RequiredArgsConstructor
public class ArmadoDeJugadores {

    private final SocioRepository socioRepository;

    public List<ReservaJugador> armar(Reserva reserva, Socio titular, List<JugadorRequest> acompaniantes) {
        Map<Long, Socio> registrados = socioRepository.findAllById(idsRegistrados(acompaniantes)).stream()
                .collect(Collectors.toMap(Socio::getId, Function.identity()));
        Set<Long> yaIncluidos = new HashSet<>(Set.of(titular.getId()));
        List<ReservaJugador> jugadores = new ArrayList<>();
        jugadores.add(conSocio(reserva, titular, true));
        acompaniantes.forEach(acompaniante -> jugadores.add(acompaniante(reserva, acompaniante, registrados, yaIncluidos)));
        return jugadores;
    }

    private List<Long> idsRegistrados(List<JugadorRequest> acompaniantes) {
        return acompaniantes.stream().map(JugadorRequest::personaId).filter(Objects::nonNull).toList();
    }

    private ReservaJugador acompaniante(Reserva reserva, JugadorRequest pedido, Map<Long, Socio> registrados, Set<Long> yaIncluidos) {
        if (pedido.personaId() == null) {
            return invitado(reserva, pedido);
        }
        Socio socio = registrados.get(pedido.personaId());
        if (socio == null || !Boolean.TRUE.equals(socio.getActivo())) {
            throw invalido("Hay un jugador que no está registrado o está deshabilitado");
        }
        if (!yaIncluidos.add(socio.getId())) {
            throw invalido("Un jugador figura más de una vez");
        }
        return conSocio(reserva, socio, false);
    }

    private ReservaJugador invitado(Reserva reserva, JugadorRequest pedido) {
        if (vacio(pedido.nombre()) || vacio(pedido.apellido())) {
            throw invalido("Cada invitado necesita nombre y apellido");
        }
        ReservaJugador jugador = new ReservaJugador();
        jugador.setReserva(reserva);
        jugador.setEsTitular(false);
        jugador.setNombre(pedido.nombre());
        jugador.setApellido(pedido.apellido());
        jugador.setDocumento(vacio(pedido.documento()) ? null : pedido.documento());
        return jugador;
    }

    private ReservaJugador conSocio(Reserva reserva, Socio socio, boolean titular) {
        ReservaJugador jugador = new ReservaJugador();
        jugador.setReserva(reserva);
        jugador.setSocio(socio);
        jugador.setEsTitular(titular);
        return jugador;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private SolicitudInvalidaException invalido(String mensaje) {
        return new SolicitudInvalidaException(CodigoError.RESERVA_JUGADORES_INVALIDOS, mensaje);
    }
}
