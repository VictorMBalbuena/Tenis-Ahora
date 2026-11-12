package com.tenisahora.modules.reserva.service;

import java.math.BigDecimal;

/** Lo que BACK 08 usa cuando se paga la seña: pasa la reserva de pendiente a confirmada. No tiene endpoint. */
public interface ConfirmacionDeReserva {

    /** Idempotente: confirmar una reserva ya confirmada no hace nada. Falla si está cancelada (por ejemplo, vencida) o finalizada. */
    void confirmar(Long reservaId);

    /** Para un pago que ya entró: si la reserva se canceló no la revive, deja el monto como reembolso y devuelve false. */
    boolean confirmarPagoAcreditado(Long reservaId, BigDecimal monto);

    /** Si se anula la seña de una reserva confirmada, vuelve a pendiente con un plazo nuevo para pagar. */
    void volverAPendiente(Long reservaId);
}
