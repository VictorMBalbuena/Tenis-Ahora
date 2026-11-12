package com.tenisahora.shared.exception;

/** HTTP 400: el pedido es inválido. */
public class SolicitudInvalidaException extends ErrorApiException {

    public SolicitudInvalidaException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
