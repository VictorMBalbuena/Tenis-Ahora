package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.AvisoDePagoDescartado;
import com.tenisahora.modules.cobro.repository.AvisoDePagoDescartadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Guarda los avisos no auténticos para revisarlos (CU78 A1). Lo que trae el aviso no es confiable: se recorta al largo de cada columna. */
@Component
@RequiredArgsConstructor
public class RegistroDeAvisosDescartados {

    private static final int LARGO_ID_PAGO = 60;
    private static final int LARGO_ID_PEDIDO = 100;

    private final AvisoDePagoDescartadoRepository avisoRepository;
    private final Clock clock;

    @Transactional
    public void registrar(String idPago, String idPedido) {
        AvisoDePagoDescartado aviso = new AvisoDePagoDescartado();
        aviso.setIdPago(recortar(idPago, LARGO_ID_PAGO));
        aviso.setIdPedido(recortar(idPedido, LARGO_ID_PEDIDO));
        aviso.setFechaRecepcion(LocalDateTime.now(clock));
        avisoRepository.save(aviso);
    }

    private String recortar(String texto, int largo) {
        return texto == null || texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
