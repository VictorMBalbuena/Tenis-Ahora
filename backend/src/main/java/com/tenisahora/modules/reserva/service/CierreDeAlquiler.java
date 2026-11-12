package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.insumo.service.InsumoStock;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Cierra un alquiler: devuelve al stock lo que se descontó y pasa la reserva a FINALIZADA. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreDeAlquiler {

    private final ReservaRepository reservaRepository;
    private final ReservaInsumoRepository reservaInsumoRepository;
    private final InsumoStock insumoStock;
    private final Clock clock;

    /** Cierre pedido por una persona: tiene que estar en curso. */
    @Transactional
    public Reserva cerrar(Long reservaId, Long personaId) {
        Reserva reserva = bloquear(reservaId);
        if (!reserva.getEstado().puedeFinalizarse()) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_FINALIZABLE,
                    "La reserva está " + reserva.getEstado().etiqueta() + ": solo se finaliza un alquiler en curso");
        }
        return reponerYFinalizar(reserva, personaId, "fin del alquiler");
    }

    /** Cierre automático de una reserva vencida, en su propia transacción: si una falla, las demás siguen. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cerrarVencida(Long reservaId) {
        Reserva reserva = bloquear(reservaId);
        if (reserva.getEstado() == EstadoReserva.EN_CURSO) {
            reponerYFinalizar(reserva, null, "finalización automática");
            log.info("Reserva {} finalizada automáticamente", reservaId);
        }
    }

    private Reserva reponerYFinalizar(Reserva reserva, Long personaId, String motivo) {
        Long id = reserva.getId();
        String motivoCompleto = "Reserva " + ReservaMapper.numeroDe(id) + ": " + motivo;
        List<ReservaInsumo> enUso = reservaInsumoRepository.deLaReserva(id).stream()
                .filter(asignado -> Boolean.TRUE.equals(asignado.getDescontado()) && !Boolean.TRUE.equals(asignado.getRepuesto())).toList();
        enUso.forEach(asignado -> insumoStock.reponer(asignado.getInsumo().getId(), asignado.getCantidadAsignada(), motivoCompleto, personaId));
        reservaInsumoRepository.marcarRepuestos(id);
        // reponer renueva la sesión de la base: la reserva se vuelve a leer para cambiarle el estado
        Reserva actual = reservaRepository.findById(id).orElseThrow();
        actual.setEstado(EstadoReserva.FINALIZADA);
        actual.setFinReal(LocalDateTime.now(clock));
        return reservaRepository.saveAndFlush(actual);
    }

    private Reserva bloquear(Long reservaId) {
        return reservaRepository.buscarParaModificar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
    }
}
