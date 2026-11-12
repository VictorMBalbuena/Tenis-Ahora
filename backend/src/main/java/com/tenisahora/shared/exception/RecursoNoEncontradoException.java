package com.tenisahora.shared.exception;

/** HTTP 404: el recurso pedido no existe. */
public class RecursoNoEncontradoException extends ErrorApiException {

    public RecursoNoEncontradoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
