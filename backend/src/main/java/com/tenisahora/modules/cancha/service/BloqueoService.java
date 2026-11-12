package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.BloqueoDelDiaResponse;
import com.tenisahora.modules.cancha.dto.BloqueoRequest;
import com.tenisahora.modules.cancha.dto.BloqueoResponse;

import java.time.LocalDate;
import java.util.List;

public interface BloqueoService {

    /** CU091: bloquea la cancha por mantenimiento; si hay reservas vigentes (confirmadas o pendientes de pago) en el rango, avisa y no bloquea. */
    BloqueoResponse bloquear(Long canchaId, BloqueoRequest request, Long actorId);

    /** Días bloqueados de la cancha entre las dos fechas inclusive (hasta 90 días), para poder levantarlos. */
    List<BloqueoDelDiaResponse> listar(Long canchaId, LocalDate desde, LocalDate hasta);

    /** Levanta el bloqueo de un día: la cancha vuelve a figurar libre en esa franja. */
    void desbloquear(Long canchaId, Long bloqueoId, Long actorId);
}
