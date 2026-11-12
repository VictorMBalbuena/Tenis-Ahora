package com.tenisahora.shared.exception;

import java.util.Map;

/** HTTP 422: incumple una regla de negocio. */
public class ReglaDeNegocioException extends ErrorApiException {

    public ReglaDeNegocioException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public ReglaDeNegocioException(CodigoError codigo, String mensaje, Map<String, String> campos) {
        super(codigo, mensaje, campos);
    }
}
