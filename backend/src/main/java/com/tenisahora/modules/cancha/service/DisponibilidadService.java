package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.GrillaResponse;

import java.time.LocalDate;

public interface DisponibilidadService {

    /** CU092: grilla de horarios libres, ocupados y bloqueados de la cancha entre las dos fechas inclusive. */
    GrillaResponse consultar(Long canchaId, LocalDate desde, LocalDate hasta);
}
