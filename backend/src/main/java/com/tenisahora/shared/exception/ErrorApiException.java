package com.tenisahora.shared.exception;

import java.util.Map;

/** Base de las excepciones de negocio: llevan un código estable que viaja en el ErrorResponse. */
public abstract class ErrorApiException extends RuntimeException {

    private final CodigoError codigo;
    private final Map<String, String> campos;

    protected ErrorApiException(CodigoError codigo, String mensaje) {
        this(codigo, mensaje, null);
    }

    /** Cuando el error señala un campo puntual del pedido (ej. "passwordActual"), igual que una falla de @Valid. */
    protected ErrorApiException(CodigoError codigo, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.codigo = codigo;
        this.campos = campos;
    }

    public CodigoError getCodigoError() {
        return codigo;
    }

    /** El código como texto, tal como viaja en el JSON. */
    public String getCodigo() {
        return codigo.name();
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
