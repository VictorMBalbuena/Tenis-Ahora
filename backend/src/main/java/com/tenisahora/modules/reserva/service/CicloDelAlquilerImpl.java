package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.insumo.service.InsumoStock;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaInsumo;
import com.tenisahora.modules.reserva.mapper.ReservaMapper;
import com.tenisahora.modules.reserva.repository.ReservaInsumoRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CicloDelAlquilerImpl implements CicloDelAlquiler {

    private final ReservaRepository reservaRepository;
    private final ReservaInsumoRepository reservaInsumoRepository;
    private final InsumoStock insumoStock;
    private final CierreDeAlquiler cierre;
    private final ArmadoDeRespuestas respuestas;
    private final ReservasProperties propiedades;
    private final Clock clock;

    @Override
    @Transactional
    public ReservaResponse iniciar(Long reservaId, Actor actor) {
        Reserva reserva = reservaRepository.buscarParaModificar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        LocalDateTime ahora = LocalDateTime.now(clock);
        asegurarQueSePuedeIniciar(reserva, ahora);
        descontarLosInsumos(reserva, actor.id());
        // descontar renueva la sesión de la base: la reserva se vuelve a leer para cambiarle el estado
        Reserva actual = reservaRepository.findById(reservaId).orElseThrow();
        actual.setEstado(EstadoReserva.EN_CURSO);
        actual.setInicioReal(ahora);
        reservaRepository.saveAndFlush(actual);
        log.info("Alquiler de la reserva {} iniciado por la persona {}", reservaId, actor.id());
        return respuestas.deReserva(actual);
    }

    @Override
    @Transactional
    public ReservaResponse finalizar(Long reservaId, Actor actor) {
        Reserva finalizada = cierre.cerrar(reservaId, actor.id());
        log.info("Alquiler de la reserva {} finalizado por la persona {}", reservaId, actor.id());
        return respuestas.deReserva(finalizada);
    }

    @Override
    public int finalizarVencidas() {
        LocalDateTime limite = LocalDateTime.now(clock).minusMinutes(propiedades.margenFinalizacionMinutos());
        List<Long> vencidas = reservaRepository.terminadasAntesDe(EstadoReserva.EN_CURSO, limite.toLocalDate(), limite.toLocalTime())
                .stream().map(Reserva::getId).toList();
        int finalizadas = 0;
        for (Long id : vencidas) {
            try {
                cierre.cerrarVencida(id);
                finalizadas++;
            } catch (RuntimeException error) {
                log.error("No se pudo finalizar sola la reserva {}: {}", id, error.getMessage());
            }
        }
        return finalizadas;
    }

    /** Solo una reserva con la seña paga, desde unos minutos antes de su turno y hasta que termina (CU14). */
    private void asegurarQueSePuedeIniciar(Reserva reserva, LocalDateTime ahora) {
        if (!reserva.getEstado().puedeIniciarse()) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_INICIABLE,
                    "La reserva está " + reserva.getEstado().etiqueta() + ": solo se inicia una reserva confirmada");
        }
        LocalDateTime desde = reserva.getFecha().atTime(reserva.getHoraInicio()).minusMinutes(propiedades.minutosAntesParaIniciar());
        if (ahora.isBefore(desde) || !ahora.isBefore(reserva.getFecha().atTime(reserva.getHoraFin()))) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_INICIABLE, "El alquiler se inicia desde "
                    + propiedades.minutosAntesParaIniciar() + " minutos antes del turno y hasta que termina");
        }
    }

    /** Saca del stock cada insumo asignado. Si uno no alcanza, falla todo y el stock queda como estaba. */
    private void descontarLosInsumos(Reserva reserva, Long personaId) {
        String motivo = "Reserva " + ReservaMapper.numeroDe(reserva.getId()) + ": inicio del alquiler";
        List<ReservaInsumo> asignados = reservaInsumoRepository.deLaReserva(reserva.getId()).stream()
                .filter(asignado -> !Boolean.TRUE.equals(asignado.getDescontado()) && !Boolean.TRUE.equals(asignado.getRepuesto())).toList();
        asignados.forEach(asignado -> insumoStock.descontar(asignado.getInsumo().getId(), asignado.getCantidadAsignada(), motivo, personaId));
        reservaInsumoRepository.marcarDescontados(reserva.getId());
    }
}
