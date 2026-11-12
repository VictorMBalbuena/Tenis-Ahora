package com.tenisahora.infrastructure.archivos;

/** La imagen no cumple el formato o el tamaño exigidos. infrastructure no conoce ErrorApiException (es de shared);
 * quien llama (MiPerfilService) la traduce a la respuesta 422 de la API. */
public class FotoInvalidaException extends RuntimeException {

    public FotoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
