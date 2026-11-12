package com.tenisahora.modules.reserva.job;

import com.tenisahora.modules.reserva.service.CicloDelAlquiler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Finaliza las reservas en curso que nadie cerró, para que sus insumos vuelvan al stock en lugar de quedar descontados para siempre. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalizacionAutomaticaJob {

    private final CicloDelAlquiler cicloDelAlquiler;

    @Scheduled(fixedDelayString = "${app.reservas.intervalo-finalizacion-ms:60000}")
    public void finalizarVencidas() {
        int finalizadas = cicloDelAlquiler.finalizarVencidas();
        if (finalizadas > 0) {
            log.info("Finalización automática de alquileres: {} finalizados", finalizadas);
        }
    }
}
