package com.tenisahora.modules.reserva.repository;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/** Filtros combinables de la búsqueda de reservas: cada uno se agrega solo si el pedido lo trae. */
public final class ReservaSpecs {

    private ReservaSpecs() {
    }

    public static Specification<Reserva> conId(Long id) {
        return (reserva, consulta, cb) -> cb.equal(reserva.get("id"), id);
    }

    public static Specification<Reserva> deEstado(EstadoReserva estado) {
        return (reserva, consulta, cb) -> cb.equal(reserva.get("estado"), estado);
    }

    public static Specification<Reserva> deCancha(Long canchaId) {
        return (reserva, consulta, cb) -> cb.equal(reserva.get("cancha").get("id"), canchaId);
    }

    public static Specification<Reserva> enFecha(LocalDate fecha) {
        return (reserva, consulta, cb) -> cb.equal(reserva.get("fecha"), fecha);
    }

    /** Desde hoy en adelante (true) o anteriores a hoy (false). */
    public static Specification<Reserva> proximas(boolean proximas, LocalDate hoy) {
        return (reserva, consulta, cb) -> proximas
                ? cb.greaterThanOrEqualTo(reserva.<LocalDate>get("fecha"), hoy)
                : cb.lessThan(reserva.<LocalDate>get("fecha"), hoy);
    }

    /** Reservas donde la persona es titular o jugadora registrada. */
    public static Specification<Reserva> participa(Long personaId) {
        return (reserva, consulta, cb) -> {
            Subquery<Long> sub = consulta.subquery(Long.class);
            Root<ReservaJugador> jugador = sub.from(ReservaJugador.class);
            sub.select(jugador.get("id")).where(cb.equal(jugador.get("reserva"), reserva),
                    cb.equal(jugador.get("socio").get("id"), personaId));
            return cb.exists(sub);
        };
    }

    /** Reservas cuyo titular tiene ese documento. */
    public static Specification<Reserva> titularConDni(String dni) {
        return (reserva, consulta, cb) -> {
            Subquery<Long> sub = consulta.subquery(Long.class);
            Root<ReservaJugador> jugador = sub.from(ReservaJugador.class);
            sub.select(jugador.get("id")).where(cb.equal(jugador.get("reserva"), reserva),
                    cb.isTrue(jugador.get("esTitular")), cb.equal(jugador.get("socio").get("dni"), dni));
            return cb.exists(sub);
        };
    }
}
