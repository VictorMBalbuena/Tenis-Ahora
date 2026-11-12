package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Recupera los pagos cuyo aviso del webhook se perdió. Hace falta porque el webhook responde 200 antes de
 * procesar (para no hacer esperar a Mercado Pago): si el procesamiento asíncrono falla, Mercado Pago ya dio el
 * aviso por entregado y no reintenta. Sin esta pieza, ese pago quedaría cobrado sin que la reserva se confirme.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConciliacionDePagos {

    private final CobroRepository cobroRepository;
    private final AcreditacionDePagos acreditacionDePagos;
    private final Clock clock;

    public int conciliar() {
        List<String> pendientes = cobroRepository.pendientesDeConciliar(EstadoCobro.queEsperanAcreditacion(), LocalDateTime.now(clock))
                .stream().map(Cobro::getIdPagoExterno).toList();
        int revisados = 0;
        for (String idExterno : pendientes) {
            try {
                acreditacionDePagos.acreditar(idExterno);
                revisados++;
            } catch (RuntimeException ex) {
                log.error("No se pudo conciliar el pago {}: {}", idExterno, ex.getMessage());
            }
        }
        return revisados;
    }
}
