package com.tenisahora.shared.exception;

/** HTTP 409: choca con algo que ya existe o tiene datos asociados. */
public class ConflictoException extends ErrorApiException {

    public ConflictoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
