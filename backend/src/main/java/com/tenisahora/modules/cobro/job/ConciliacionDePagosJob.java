package com.tenisahora.modules.cobro.job;

import com.tenisahora.modules.cobro.service.ConciliacionDePagos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConciliacionDePagosJob {

    private final ConciliacionDePagos conciliacionDePagos;

    @Scheduled(fixedDelayString = "${app.cobros.intervalo-conciliacion-ms:300000}")
    public void conciliar() {
        int revisados = conciliacionDePagos.conciliar();
        if (revisados > 0) {
            log.info("Conciliación de pagos: {} cobros revisados", revisados);
        }
    }
}
