package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmacionDeReservaImpl implements ConfirmacionDeReserva {

    private final ReservaRepository reservaRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void confirmar(Long reservaId) {
        Reserva reserva = reservaRepository.buscarParaModificar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            return;
        }
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_MODIFICABLE,
                    "La reserva está " + reserva.getEstado().etiqueta() + ": ya no se puede confirmar");
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);
        log.info("Reserva {} confirmada", reservaId);
    }

    @Override
    @Transactional
    public boolean confirmarPagoAcreditado(Long reservaId, BigDecimal monto) {
        Reserva reserva = reservaRepository.buscarParaModificar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            reserva.setMontoReembolso(monto);
            reservaRepository.save(reserva);
            return false;
        }
        if (reserva.getEstado() == EstadoReserva.PENDIENTE_PAGO) {
            confirmar(reservaId);
        }
        return true;
    }

    @Override
    @Transactional
    public void volverAPendiente(Long reservaId) {
        Reserva reserva = reservaRepository.buscarParaModificar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
            reserva.setPlazoPagoDesde(LocalDateTime.now(clock));
            reservaRepository.save(reserva);
        }
    }
}
