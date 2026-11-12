package com.tenisahora.modules.torneo.job;

import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VencimientoDeInscripcionesTorneoJob {

    private final ConfirmacionDeInscripcionTorneo confirmacion;

    @Scheduled(fixedDelayString = "${app.torneos.intervalo-vencimiento-ms:60000}")
    public void vencerPendientes() {
        confirmacion.vencerPendientes();
    }
}
