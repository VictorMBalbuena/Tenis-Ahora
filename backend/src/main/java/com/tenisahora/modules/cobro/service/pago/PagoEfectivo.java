package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import org.springframework.stereotype.Component;

/** Efectivo en la caja del club: no pasa por ninguna terminal, queda pagado en el acto y el número de operación es opcional. */
@Component
public class PagoEfectivo implements MedioDePago {

    @Override
    public MedioPago medio() {
        return MedioPago.EFECTIVO;
    }

    @Override
    public ResultadoDeCobro cobrar(Cobro cobro, DatosDeCobro datos) {
        String comprobante = datos.numeroOperacion() == null || datos.numeroOperacion().isBlank() ? null : datos.numeroOperacion();
        return ResultadoDeCobro.pagado(comprobante);
    }
}
