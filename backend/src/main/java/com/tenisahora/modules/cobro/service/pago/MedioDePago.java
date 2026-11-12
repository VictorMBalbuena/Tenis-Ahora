package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;

/**
 * Un medio de pago habilitado (RD23): sabe cobrar y a qué {@link MedioPago} responde. Débito y crédito quedan
 * resueltos en el acto; Mercado Pago deja el cobro pendiente del aviso. Un medio nuevo es una clase nueva que
 * implementa esta interfaz, sin tocar las demás (OCP).
 */
public interface MedioDePago {

    MedioPago medio();

    /** Intenta cobrar. No persiste el cobro: solo calcula en qué queda y, para QR, pide la orden a la pasarela. */
    ResultadoDeCobro cobrar(Cobro cobro, DatosDeCobro datos);
}
