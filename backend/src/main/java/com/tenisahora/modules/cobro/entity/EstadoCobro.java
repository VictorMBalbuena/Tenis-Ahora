package com.tenisahora.modules.cobro.entity;

import java.util.Arrays;
import java.util.List;

public enum EstadoCobro {
    PENDIENTE,
    PAGADO,
    VENCIDO,
    RECHAZADO,
    ANULADO;

    /** Un cobro sin acreditar admite un QR nuevo: el anterior venció o lo rechazaron (CU88 A1, A2). */
    public boolean puedeGenerarQr() {
        return this == PENDIENTE || this == VENCIDO || this == RECHAZADO;
    }

    /** Solo se acredita el pago que el sistema está esperando: un aviso repetido no registra de nuevo (CU78 A2). */
    public boolean puedeAcreditarse() {
        return this == PENDIENTE;
    }

    /** Tras un rechazo el link sigue abierto, y un pago en proceso puede aprobarse después de vencer el QR: esos pagos también se acreditan. */
    public boolean admiteAviso() {
        return this == PENDIENTE || this == RECHAZADO || this == VENCIDO;
    }

    /** Pagado y anulado no cambian más; los demás admiten otro intento. */
    public boolean esFinal() {
        return this == PAGADO || this == ANULADO;
    }

    /** El recibo es obligatorio, y solo existe para un cobro efectivamente pagado (RD09, CU57). */
    public boolean emiteRecibo() {
        return this == PAGADO;
    }

    /** Ya hay un cobro en curso o resuelto a favor de la reserva: no corresponde registrar otro (CU51, 409). */
    public boolean bloqueaNuevoCobro() {
        return this == PENDIENTE || this == PAGADO;
    }

    /** Los estados que esperan el aviso de Mercado Pago, para el vencimiento del QR y la conciliación. */
    public static List<EstadoCobro> queEsperanAcreditacion() {
        return Arrays.stream(values()).filter(EstadoCobro::puedeAcreditarse).toList();
    }

    /** Los estados que bloquean un nuevo cobro para la misma reserva. */
    public static List<EstadoCobro> queBloqueanNuevoCobro() {
        return Arrays.stream(values()).filter(EstadoCobro::bloqueaNuevoCobro).toList();
    }
}
