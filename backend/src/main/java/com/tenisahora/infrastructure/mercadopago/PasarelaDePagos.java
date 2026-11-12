package com.tenisahora.infrastructure.mercadopago;

/**
 * Lo que el sistema necesita de Mercado Pago: pedir un QR y preguntar cómo quedó el pago (CU88, CU78).
 * Habla con tipos propios y no conoce ningún módulo, así que el resto del código se puede probar con un doble
 * de prueba y la pasarela se puede cambiar sin tocar los cobros.
 */
public interface PasarelaDePagos {

    /** Pide una orden con QR por el monto indicado. La clave de idempotencia evita crear dos órdenes si se reintenta. */
    OrdenQr crearOrdenQr(SolicitudDeOrden solicitud);

    /**
     * Pregunta a Mercado Pago cómo quedó la orden. Se consulta siempre: el aviso dice que algo pasó, pero el estado
     * que vale es el que responde la pasarela (CU78).
     */
    EstadoDeOrden consultar(String idExterno);

    /** Vence ya una orden abierta, para que nadie pague un QR que se reemplazó por otro medio de pago. */
    void cerrarOrden(String idExterno);

    /**
     * El aviso del webhook trae el id de un pago, no el de la orden. Esta consulta traduce uno en el otro: la
     * referencia externa con la que se creó la orden (el código del cobro), para encontrar a qué cobro corresponde.
     */
    String referenciaExternaDelPago(String idDePago);
}
