package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Dueño de un cobro: el personal ve cualquiera; un socio solo los de una reserva donde juega. Una reserva ajena o
 * inexistente responde 404, no 403, así no se puede averiguar qué cobros existen probando números.
 */
@Component
@RequiredArgsConstructor
public class AccesoACobros {

    private final AccesoAReservas accesoAReservas;

    public Cobro visible(Cobro cobro, Actor actor) {
        if (cobro.getReserva() == null) {
            return inscripcionVisible(cobro, actor);
        }
        // Delega la regla de "personal ve todo, socio solo si juega" en la que ya existe para reservas (CU51, CU77).
        accesoAReservas.visible(cobro.getReserva().getId(), actor);
        return cobro;
    }

    private Cobro inscripcionVisible(Cobro cobro, Actor actor) {
        boolean propio = (cobro.getInscripcionActividad() != null && cobro.getInscripcionActividad().getSocio().getId().equals(actor.id()))
                || (cobro.getInscripcionTorneo() != null && cobro.getInscripcionTorneo().participa(actor.id()));
        if (!propio && !AccesoAReservas.esPersonal(actor)) {
            throw noEncontrado();
        }
        return cobro;
    }

    private RecursoNoEncontradoException noEncontrado() {
        return new RecursoNoEncontradoException(CodigoError.COBRO_NO_ENCONTRADO, "Cobro no encontrado");
    }
}
