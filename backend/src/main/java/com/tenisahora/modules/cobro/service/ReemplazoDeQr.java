package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.dto.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Saca del medio un QR sin pagar cuando se cobra de otra forma: primero mira si ya lo pagaron, para no cobrar dos veces. */
@Component
@RequiredArgsConstructor
public class ReemplazoDeQr {

    private static final String MOTIVO = "Reemplazado por otro medio de pago";

    private final AcreditacionDePagos acreditacionDePagos;
    private final CierreDeOrden cierreDeOrden;
    private final RegistroDeAnulacion registroDeAnulacion;

    public void reemplazar(Cobro cobro, Actor actor) {
        acreditacionDePagos.acreditar(cobro.getIdPagoExterno());
        if (!cobro.getEstado().admiteAviso()) {
            return;
        }
        registroDeAnulacion.aplicar(cobro, MOTIVO, actor.id());
        cierreDeOrden.cerrar(cobro.getIdPagoExterno());
    }

    /** El link de un QR rechazado sigue abierto: si no se cierra, alguien podría pagarlo después de cobrar con otro intento. */
    public void reemplazarRechazados(List<Cobro> rechazados, Actor actor) {
        rechazados.stream()
                .filter(cobro -> cobro.getMedioPago() == MedioPago.QR && cobro.getIdPagoExterno() != null)
                .forEach(cobro -> reemplazar(cobro, actor));
    }
}
