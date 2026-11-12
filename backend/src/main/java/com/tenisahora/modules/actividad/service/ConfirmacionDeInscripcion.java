package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/** Lo que el pago le hace a la inscripción: la seña la confirma y anular esa seña la devuelve a pendiente. */
@Component
@RequiredArgsConstructor
public class ConfirmacionDeInscripcion {

    private final InscripcionClaseRepository inscripcionRepository;
    private final Clock clock;

    @Transactional
    public void confirmar(Long inscripcionId) {
        InscripcionClase inscripcion = buscar(inscripcionId);
        if (inscripcion.getEstado() == EstadoInscripcion.PENDIENTE_PAGO) {
            inscripcion.setEstado(EstadoInscripcion.CONFIRMADA);
            inscripcionRepository.save(inscripcion);
        }
    }

    /** Falso si el pago llegó con la inscripción ya vencida: el lugar se liberó y hay que devolver la plata. */
    @Transactional
    public boolean confirmarPagoAcreditado(Long inscripcionId, BigDecimal monto) {
        InscripcionClase inscripcion = buscar(inscripcionId);
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            inscripcion.setMontoReembolso(monto);
            inscripcionRepository.save(inscripcion);
            return false;
        }
        confirmar(inscripcionId);
        return true;
    }

    @Transactional
    public void volverAPendiente(Long inscripcionId) {
        InscripcionClase inscripcion = buscar(inscripcionId);
        if (inscripcion.getEstado() == EstadoInscripcion.CONFIRMADA) {
            inscripcion.setEstado(EstadoInscripcion.PENDIENTE_PAGO);
            inscripcion.setFechaCreacion(LocalDateTime.now(clock));
            inscripcionRepository.save(inscripcion);
        }
    }

    private InscripcionClase buscar(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.INSCRIPCION_NO_ENCONTRADA, "Inscripción no encontrada"));
    }
}
