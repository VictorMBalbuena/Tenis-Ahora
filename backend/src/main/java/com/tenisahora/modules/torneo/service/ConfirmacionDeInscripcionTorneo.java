package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Lo que el pago del arancel le hace a la inscripción, y el vencimiento de las que nunca se pagaron. */
@Component
@RequiredArgsConstructor
public class ConfirmacionDeInscripcionTorneo {

    private static final int MINUTOS_PARA_VENCER = 30;

    private final InscripcionRepository inscripcionRepository;
    private final Clock clock;

    public static LocalDateTime venceEn(Inscripcion inscripcion) {
        if (inscripcion.getEstado() != EstadoInscripcionTorneo.PENDIENTE_PAGO || inscripcion.getFechaCreacion() == null) {
            return null;
        }
        return inscripcion.getFechaCreacion().plusMinutes(MINUTOS_PARA_VENCER);
    }

    @Transactional
    public void confirmar(Long id) {
        Inscripcion inscripcion = buscar(id);
        if (inscripcion.getEstado() == EstadoInscripcionTorneo.PENDIENTE_PAGO) {
            inscripcion.setEstado(EstadoInscripcionTorneo.CONFIRMADA);
            inscripcionRepository.save(inscripcion);
        }
    }

    /** Falso si el pago llegó con la inscripción ya vencida: hay que devolver la plata. */
    @Transactional
    public boolean confirmarPagoAcreditado(Long id) {
        if (buscar(id).getEstado() == EstadoInscripcionTorneo.CANCELADA) {
            return false;
        }
        confirmar(id);
        return true;
    }

    @Transactional
    public void volverAPendiente(Long id) {
        Inscripcion inscripcion = buscar(id);
        if (inscripcion.getEstado() == EstadoInscripcionTorneo.CONFIRMADA) {
            inscripcion.setEstado(EstadoInscripcionTorneo.PENDIENTE_PAGO);
            inscripcion.setFechaCreacion(LocalDateTime.now(clock));
            inscripcionRepository.save(inscripcion);
        }
    }

    @Transactional
    public int vencerPendientes() {
        List<Inscripcion> vencidas = inscripcionRepository.pendientesCreadasAntesDe(LocalDateTime.now(clock).minusMinutes(MINUTOS_PARA_VENCER));
        vencidas.forEach(inscripcion -> inscripcion.setEstado(EstadoInscripcionTorneo.CANCELADA));
        inscripcionRepository.saveAll(vencidas);
        return vencidas.size();
    }

    private Inscripcion buscar(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.INSCRIPCION_NO_ENCONTRADA, "Inscripción no encontrada"));
    }
}
