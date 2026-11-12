package com.tenisahora.infrastructure.mercadopago;

/**
 * La pasarela no contestó o contestó un error. Envuelve las fallas del SDK para que los módulos no dependan de
 * sus tipos, y nunca lleva datos del pedido en el mensaje.
 */
public class PasarelaNoDisponibleException extends RuntimeException {

    public PasarelaNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public PasarelaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
