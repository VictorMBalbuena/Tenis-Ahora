package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.modules.reserva.repository.ReservaSpecs;
import com.tenisahora.shared.event.CuentaDeshabilitadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cancela los turnos futuros de quien queda deshabilitado, si el administrador lo pidió (CU76, RF76).
 *
 * <p>Solo se tocan las reservas que todavía se pueden cancelar y que son de hoy en adelante: el historial no se
 * modifica. No se cobra penalidad: la cancelación no la pidió el socio, se la impuso el club.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservasDeCuentaDeshabilitadaListener {

    private final ReservaRepository reservaRepository;
    private final Clock clock;

    @EventListener
    public void alDeshabilitar(CuentaDeshabilitadaEvent evento) {
        if (!evento.cancelarReservasFuturas()) {
            return;
        }
        LocalDate hoy = LocalDate.now(clock);
        Specification<Reserva> filtro = Specification.where(ReservaSpecs.participa(evento.personaId()))
                .and(ReservaSpecs.proximas(true, hoy));

        List<Reserva> futuras = reservaRepository.findAll(filtro).stream()
                .filter(reserva -> reserva.getEstado().puedeCancelarse())
                .toList();

        futuras.forEach(reserva -> {
            reserva.setEstado(EstadoReserva.CANCELADA);
            reserva.setFechaCancelacion(LocalDateTime.now(clock));
        });
        reservaRepository.saveAll(futuras);

        log.info("Canceladas {} reservas futuras de la persona {} por deshabilitación", futuras.size(),
                evento.personaId());
    }
}
