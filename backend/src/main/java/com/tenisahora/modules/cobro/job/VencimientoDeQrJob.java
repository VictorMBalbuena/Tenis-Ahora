package com.tenisahora.modules.cobro.job;

import com.tenisahora.modules.cobro.service.VencimientoDeQr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientoDeQrJob {

    private final VencimientoDeQr vencimientoDeQr;

    @Scheduled(fixedDelayString = "${app.cobros.intervalo-vencimiento-qr-ms:60000}")
    public void vencerExpirados() {
        int vencidos = vencimientoDeQr.vencerExpirados();
        if (vencidos > 0) {
            log.info("Vencimiento de QR de cobros: {} vencidos", vencidos);
        }
    }
}
