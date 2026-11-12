package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Va en su propio componente porque {@code @Transactional} solo actúa si se llama desde afuera de la clase. Antes de
 * dar el QR por vencido vuelve a consultar a Mercado Pago: si el pago se acreditó justo en el límite, no lo marca VENCIDO.
 */
@Component
@RequiredArgsConstructor
public class VencimientoDeUnQr {

    private final CobroRepository cobroRepository;
    private final AcreditacionDePagos acreditacionDePagos;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void vencer(Long cobroId) {
        Cobro cobro = cobroRepository.buscarParaModificar(cobroId).orElseThrow();
        if (!cobro.getEstado().puedeAcreditarse()) {
            return;
        }
        acreditacionDePagos.acreditar(cobro.getIdPagoExterno());
        Cobro actual = cobroRepository.buscarParaModificar(cobroId).orElseThrow();
        if (actual.getEstado().puedeAcreditarse()) {
            actual.setEstado(EstadoCobro.VENCIDO);
            cobroRepository.save(actual);
        }
    }
}
