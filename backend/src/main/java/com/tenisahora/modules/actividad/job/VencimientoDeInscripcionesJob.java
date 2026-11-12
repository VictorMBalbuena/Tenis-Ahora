package com.tenisahora.modules.actividad.job;

import com.tenisahora.modules.actividad.service.VencimientoDeInscripciones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientoDeInscripcionesJob {

    private final VencimientoDeInscripciones vencimiento;

    @Scheduled(fixedDelayString = "${app.inscripciones.intervalo-vencimiento-ms:60000}")
    public void vencerPendientes() {
        int canceladas = vencimiento.vencer();
        if (canceladas > 0) {
            log.info("Vencimiento de inscripciones sin seña: {} canceladas", canceladas);
        }
    }
}
