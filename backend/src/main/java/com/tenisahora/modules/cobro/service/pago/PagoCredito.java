package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.service.CobrosProperties;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Crédito: aprueba o rechaza en el acto, igual que débito, pero admite cuotas (CU87). */
@Component
@RequiredArgsConstructor
public class PagoCredito implements MedioDePago {

    private static final int UNA_CUOTA = 1;

    private final CobrosProperties propiedades;

    @Override
    public MedioPago medio() {
        return MedioPago.CREDITO;
    }

    @Override
    public ResultadoDeCobro cobrar(Cobro cobro, DatosDeCobro datos) {
        if (datos.numeroOperacion() == null || datos.numeroOperacion().isBlank()) {
            throw new SolicitudInvalidaException(CodigoError.NUMERO_DE_OPERACION_REQUERIDO,
                    "Indicá el número de operación que dio la terminal");
        }
        int cuotas = datos.cuotas() == null ? UNA_CUOTA : datos.cuotas();
        if (cuotas < UNA_CUOTA || cuotas > propiedades.maxCuotas()) {
            throw new ReglaDeNegocioException(CodigoError.CUOTAS_INVALIDAS,
                    "Las cuotas tienen que estar entre 1 y " + propiedades.maxCuotas());
        }
        cobro.setCuotas(cuotas);
        return datos.aprobado() ? ResultadoDeCobro.pagado(datos.numeroOperacion())
                : ResultadoDeCobro.rechazado(datos.numeroOperacion());
    }
}
