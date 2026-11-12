package com.tenisahora.modules.cobro.service.pago;

/**
 * Lo que trae el pedido de cobro para que el medio elegido lo procese.
 *
 * @param numeroOperacion número que devuelve la terminal del club, para débito y crédito (CU86, CU87)
 * @param cuotas          cantidad de cuotas, solo para crédito
 * @param aprobado        si la terminal aprobó la operación. La terminal es física y externa a este sistema: quien
 *                        cobra carga lo que mostró. En falso deja el cobro {@code RECHAZADO}, habilitando otro
 *                        intento con otro medio (CU86/CU87 A1)
 * @param registradoPorId quién lo cobró; nulo si lo paga el socio online (QR)
 */
public record DatosDeCobro(String numeroOperacion, Integer cuotas, boolean aprobado, Long registradoPorId) {
}
