package com.tenisahora.modules.reserva.repository;

import com.tenisahora.modules.reserva.entity.ReservaJugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaJugadorRepository extends JpaRepository<ReservaJugador, Long> {

    /** El titular primero y después los acompañantes, en el orden en que se cargaron. */
    List<ReservaJugador> findByReservaIdInOrderByReservaIdAscEsTitularDescIdAsc(Collection<Long> reservaIds);

    boolean existsByReservaIdAndSocioId(Long reservaId, Long socioId);

    boolean existsByReservaIdAndEsTitularTrueAndSocioId(Long reservaId, Long socioId);

    /** El titular de la reserva, para quien se calculan los descuentos del cobro de la seña (BACK 08, RD21). */
    Optional<ReservaJugador> findByReservaIdAndEsTitularTrue(Long reservaId);
}
