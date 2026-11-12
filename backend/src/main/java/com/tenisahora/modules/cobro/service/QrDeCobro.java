package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.QrResponse;
import com.tenisahora.shared.dto.Actor;

public interface QrDeCobro {

    /** CU77/CU88: genera (o regenera, si venció o lo rechazaron) el QR de pago de un cobro. */
    QrResponse generar(Long cobroId, Actor actor);

    /** Le pregunta a Mercado Pago cómo quedó el pago y lo acredita si corresponde: confirma al instante, sin esperar el aviso. */
    CobroResponse verificar(Long cobroId, Actor actor);
}
