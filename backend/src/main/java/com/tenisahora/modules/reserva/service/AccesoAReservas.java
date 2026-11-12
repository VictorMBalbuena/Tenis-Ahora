package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaJugadorRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Quién puede ver o cancelar cada reserva. El personal accede a todas; un socio solo a las suyas, y una ajena responde 404:
 * así no se puede averiguar qué reservas existen probando números.
 */
@Component
@RequiredArgsConstructor
public class AccesoAReservas {

    private final ReservaRepository reservaRepository;
    private final ReservaJugadorRepository jugadorRepository;

    public static boolean esPersonal(Actor actor) {
        return actor.rol() == Rol.EMPLEADO || actor.rol() == Rol.ADMINISTRADOR;
    }

    /** Una reserva que el actor puede ver. */
    public Reserva visible(Long id, Actor actor) {
        Reserva reserva = reservaRepository.findById(id).orElseThrow(this::noEncontrada);
        if (!esPersonal(actor) && !jugadorRepository.existsByReservaIdAndSocioId(id, actor.id())) {
            throw noEncontrada();
        }
        return reserva;
    }

    /** Una reserva que el actor puede gestionar (cancelar, asignarle insumos): el personal cualquiera, un socio solo como titular. */
    public Reserva paraGestionar(Long id, Actor actor, boolean bloquear) {
        return paraCancelar(id, actor, bloquear);
    }

    /** Una reserva que el actor puede cancelar: el personal cualquiera, un socio solo las que tiene como titular. */
    public Reserva paraCancelar(Long id, Actor actor, boolean bloquear) {
        Reserva reserva = (bloquear ? reservaRepository.buscarParaModificar(id) : reservaRepository.findById(id))
                .orElseThrow(this::noEncontrada);
        if (esPersonal(actor) || jugadorRepository.existsByReservaIdAndEsTitularTrueAndSocioId(id, actor.id())) {
            return reserva;
        }
        if (jugadorRepository.existsByReservaIdAndSocioId(id, actor.id())) {
            throw new AccessDeniedException("Solo el titular puede cancelar la reserva");
        }
        throw noEncontrada();
    }

    private RecursoNoEncontradoException noEncontrada() {
        return new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada");
    }

    /**
     * El socio titular de la reserva, para que otro módulo le aplique beneficios de su cuenta (como los descuentos
     * de BACK 08, RD21). Nulo si el titular es un invitado sin cuenta.
     */
    public Long titularId(Long reservaId) {
        return jugadorRepository.findByReservaIdAndEsTitularTrue(reservaId)
                .map(jugador -> jugador.getSocio() != null ? jugador.getSocio().getId() : null)
                .orElse(null);
    }
}
