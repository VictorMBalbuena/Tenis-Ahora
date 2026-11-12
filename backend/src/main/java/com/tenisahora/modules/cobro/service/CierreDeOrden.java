package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Cierra un link de pago que ya no sirve, para que nadie lo pague de nuevo. Si Mercado Pago no contesta no corta la operación: el link vence solo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreDeOrden {

    private final PasarelaDePagos pasarela;

    public void cerrar(String idExterno) {
        try {
            pasarela.cerrarOrden(idExterno);
        } catch (PasarelaNoDisponibleException ex) {
            log.warn("No se pudo cerrar la orden {}: vence sola al terminar su plazo", idExterno);
        }
    }
}
