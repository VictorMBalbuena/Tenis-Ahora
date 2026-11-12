package com.tenisahora.modules.reserva.job;

import com.tenisahora.modules.reserva.service.CancelacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cancela las reservas que siguen pendientes de pago pasado el plazo, para que no retengan una cancha sin que nadie pague. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiracionDePendientesJob {

    private final CancelacionService cancelacionService;

    @Scheduled(fixedDelayString = "${app.reservas.intervalo-vencimiento-ms:60000}")
    public void vencerPendientes() {
        int canceladas = cancelacionService.cancelarVencidas();
        if (canceladas > 0) {
            log.info("Vencimiento de reservas pendientes de pago: {} canceladas", canceladas);
        }
    }
}
