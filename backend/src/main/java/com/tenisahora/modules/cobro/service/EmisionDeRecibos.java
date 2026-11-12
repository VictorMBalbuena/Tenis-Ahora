package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Emite el recibo de un cobro pagado (CU57, RD09: el recibo es obligatorio). */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmisionDeRecibos {

    private final ReciboRepository reciboRepository;

    /** Idempotente: si el cobro ya tiene recibo, no emite otro (evita duplicar con un aviso repetido, CU78 A2). */
    @Transactional
    public Recibo emitir(Cobro cobro) {
        if (!cobro.getEstado().emiteRecibo()) {
            throw new ReglaDeNegocioException(CodigoError.RECIBO_NO_EMITIBLE,
                    "El cobro está " + cobro.getEstado() + ": solo se emite recibo de un cobro pagado");
        }
        return reciboRepository.findByCobroId(cobro.getId()).orElseGet(() -> nuevo(cobro));
    }

    private String detalleDe(Cobro cobro) {
        if (cobro.getInscripcionActividad() != null) {
            return "Inscripción a " + cobro.getInscripcionActividad().getActividad().getNombre();
        }
        return cobro.getTorneo() != null ? "Inscripción al torneo " + cobro.getTorneo().getNombre() : null;
    }

    private Recibo nuevo(Cobro cobro) {
        Recibo recibo = new Recibo();
        recibo.setCobro(cobro);
        recibo.setNumeroRecibo(CobroMapper.numeroDe(cobro.getId()));
        recibo.setMonto(cobro.getMontoFinal());
        recibo.setConcepto(cobro.getConcepto().name());
        recibo.setMedioPago(cobro.getMedioPago());
        recibo.setNumeroOperacion(cobro.getNumeroOperacion());
        recibo.setObservaciones(detalleDe(cobro));
        Recibo guardado = reciboRepository.save(recibo);
        log.info("Recibo {} emitido para el cobro {}", guardado.getNumeroRecibo(), cobro.getId());
        return guardado;
    }
}
