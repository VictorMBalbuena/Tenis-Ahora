package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Component;

/** Débito: aprueba o rechaza en el acto, con el número de operación que carga quien cobra (CU86). */
@Component
public class PagoDebito implements MedioDePago {

    @Override
    public MedioPago medio() {
        return MedioPago.DEBITO;
    }

    @Override
    public ResultadoDeCobro cobrar(Cobro cobro, DatosDeCobro datos) {
        if (datos.numeroOperacion() == null || datos.numeroOperacion().isBlank()) {
            throw new SolicitudInvalidaException(CodigoError.NUMERO_DE_OPERACION_REQUERIDO,
                    "Indicá el número de operación que dio la terminal");
        }
        return datos.aprobado() ? ResultadoDeCobro.pagado(datos.numeroOperacion())
                : ResultadoDeCobro.rechazado(datos.numeroOperacion());
    }
}
