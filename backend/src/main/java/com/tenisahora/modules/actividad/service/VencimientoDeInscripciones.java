package com.tenisahora.modules.actividad.service;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** La inscripción sin seña paga en plazo se cancela sola y libera el lugar. */
@Service
@RequiredArgsConstructor
public class VencimientoDeInscripciones {

    private final InscripcionClaseRepository inscripcionRepository;
    private final InscripcionesProperties propiedades;
    private final Clock clock;

    @Transactional
    public int vencer() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<InscripcionClase> vencidas = inscripcionRepository.pendientesCreadasAntesDe(EstadoInscripcion.PENDIENTE_PAGO,
                ahora.minusMinutes(propiedades.minutosParaVencer()));
        vencidas.forEach(inscripcion -> {
            inscripcion.setActiva(false);
            inscripcion.setEstado(EstadoInscripcion.CANCELADA);
            inscripcion.setFechaCancelacion(ahora);
        });
        inscripcionRepository.saveAll(vencidas);
        return vencidas.size();
    }
}
