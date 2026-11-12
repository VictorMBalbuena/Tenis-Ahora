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

/** Pasa a VENCIDO los cobros con QR cuyo plazo pasó sin que nadie pagara, liberando el intento (CU88 A1). */
@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientoDeQr {

    private final CobroRepository cobroRepository;
    private final VencimientoDeUnQr vencimientoDeUnQr;
    private final Clock clock;

    public int vencerExpirados() {
        List<Long> vencidos = cobroRepository.conQrVencido(EstadoCobro.queEsperanAcreditacion(), LocalDateTime.now(clock))
                .stream().map(Cobro::getId).toList();
        int cantidad = 0;
        for (Long id : vencidos) {
            try {
                vencimientoDeUnQr.vencer(id);
                cantidad++;
            } catch (RuntimeException ex) {
                log.error("No se pudo vencer el cobro {}: {}", id, ex.getMessage());
            }
        }
        return cantidad;
    }
}
