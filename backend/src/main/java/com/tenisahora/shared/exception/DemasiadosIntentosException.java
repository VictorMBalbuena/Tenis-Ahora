package com.tenisahora.shared.exception;

import java.time.Duration;

/** HTTP 429: se superó un límite de intentos o de pedidos. Lleva cuánto hay que esperar (va en el encabezado Retry-After). */
public class DemasiadosIntentosException extends ErrorApiException {

    private final Duration espera;

    public DemasiadosIntentosException(CodigoError codigo, String mensaje, Duration espera) {
        super(codigo, mensaje);
        this.espera = espera;
    }

    /** Segundos de espera, redondeados para arriba y nunca menos de 1. */
    public long segundosDeEspera() {
        long segundos = espera.toSeconds() + (espera.toNanosPart() > 0 ? 1 : 0);
        return Math.max(1, segundos);
    }
}
