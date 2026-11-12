package com.tenisahora.modules.reserva.entity;

import java.util.Arrays;
import java.util.List;

public enum EstadoReserva {
    PENDIENTE_PAGO,
    CONFIRMADA,
    EN_CURSO,
    CANCELADA,
    FINALIZADA;

    /**
     * Cómo se nombra este estado en un mensaje para una persona.
     *
     * El nombre del enum va a los mensajes de error, y "PENDIENTE_PAGO" con guión bajo y en mayúsculas no es
     * algo que se le muestre a alguien que está usando el sistema.
     */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE_PAGO -> "pendiente de pago";
            case CONFIRMADA -> "confirmada";
            case EN_CURSO -> "en curso";
            case CANCELADA -> "cancelada";
            case FINALIZADA -> "finalizada";
        };
    }

    /** Pendiente de pago, confirmada y en curso ocupan la cancha; cancelada y finalizada no. */
    public boolean retieneHorario() {
        return this == PENDIENTE_PAGO || this == CONFIRMADA || this == EN_CURSO;
    }

    /** Una vez que el alquiler empezó ya no se cambia el turno ni se cancela. */
    public boolean puedeModificarse() {
        return this == PENDIENTE_PAGO || this == CONFIRMADA;
    }

    public boolean puedeCancelarse() {
        return puedeModificarse();
    }

    /** Solo una reserva con la seña paga arranca el alquiler (CU14). */
    public boolean puedeIniciarse() {
        return this == CONFIRMADA;
    }

    /** Solo una reserva pendiente de pago admite el cobro de la seña: ya confirmada, cancelada o en curso no se cobra de nuevo. */
    public boolean puedeCobrarseSenia() {
        return this == PENDIENTE_PAGO;
    }

    /** El saldo se cobra desde que la seña está paga hasta después del cierre: un turno puede terminar con deuda. */
    public boolean puedeCobrarseSaldo() {
        return this == CONFIRMADA || this == EN_CURSO || this == FINALIZADA;
    }

    public boolean puedeFinalizarse() {
        return this == EN_CURSO;
    }

    /** Los estados que ocupan la cancha, para las consultas que buscan reservas vigentes. */
    public static List<EstadoReserva> queRetienenHorario() {
        return Arrays.stream(values()).filter(EstadoReserva::retieneHorario).toList();
    }
}
