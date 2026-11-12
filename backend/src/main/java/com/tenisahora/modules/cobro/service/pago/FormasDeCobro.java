package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registro de los medios de pago habilitados: agregar uno nuevo es agregar una clase, no tocar esta (OCP). */
@Component
public class FormasDeCobro {

    private final Map<MedioPago, MedioDePago> porMedio;

    public FormasDeCobro(List<MedioDePago> medios) {
        this.porMedio = new EnumMap<>(MedioPago.class);
        medios.forEach(medio -> porMedio.put(medio.medio(), medio));
    }

    public MedioDePago de(MedioPago medio) {
        MedioDePago encontrado = porMedio.get(medio);
        if (encontrado == null) {
            throw new ReglaDeNegocioException(CodigoError.MEDIO_DE_PAGO_INVALIDO, "No hay un medio de pago habilitado para " + medio);
        }
        return encontrado;
    }
}
