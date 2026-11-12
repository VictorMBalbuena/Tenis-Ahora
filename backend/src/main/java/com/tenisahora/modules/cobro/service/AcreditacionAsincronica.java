package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Lo que dispara el webhook, en otro hilo, para responder 200 enseguida sin esperar a que termine de procesar el
 * pago (CU78). El aviso trae el id de un pago, no el de la orden: primero lo traduce a la referencia externa (el
 * código del cobro) y recién ahí llama a {@link AcreditacionDePagos}, que sigue siendo sincrónica porque
 * {@link ConciliacionDePagos} la llama una por una dentro de su propio {@code try/catch} por ítem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcreditacionAsincronica {

    private final PasarelaDePagos pasarela;
    private final CobroRepository cobroRepository;
    private final AcreditacionDePagos acreditacionDePagos;

    @Async("webhookExecutor")
    public void procesarAvisoDePago(String idDePago) {
        try {
            String referenciaExterna = pasarela.referenciaExternaDelPago(idDePago);
            Cobro cobro = cobroRepository.findByCodigo(referenciaExterna).orElse(null);
            if (cobro == null || cobro.getIdPagoExterno() == null) {
                log.warn("Aviso de Mercado Pago para un pago sin cobro asociado: {}", idDePago);
                return;
            }
            acreditacionDePagos.acreditar(cobro.getIdPagoExterno());
        } catch (RuntimeException ex) {
            log.error("Falló el procesamiento asíncrono del aviso de pago {}: {}", idDePago, ex.getMessage(), ex);
        }
    }
}
