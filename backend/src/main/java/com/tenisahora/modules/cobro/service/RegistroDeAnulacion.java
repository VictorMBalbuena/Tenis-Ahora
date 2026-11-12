package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.EstadoRecibo;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** Deja anulado un cobro con motivo, fecha y responsable, junto con su recibo. Nunca borra nada. */
@Component
@RequiredArgsConstructor
public class RegistroDeAnulacion {

    private final CobroRepository cobroRepository;
    private final ReciboRepository reciboRepository;
    private final Clock clock;

    public void aplicar(Cobro cobro, String motivo, Long responsableId) {
        cobro.setEstado(EstadoCobro.ANULADO);
        cobro.setMotivoAnulacion(motivo);
        cobro.setFechaAnulacion(LocalDateTime.now(clock));
        cobro.setAnuladoPorId(responsableId);
        cobroRepository.saveAndFlush(cobro);
        reciboRepository.findByCobroId(cobro.getId()).ifPresent(recibo -> {
            recibo.setEstado(EstadoRecibo.ANULADO);
            recibo.setObservaciones("Anulado: " + motivo);
            reciboRepository.save(recibo);
        });
    }
}
