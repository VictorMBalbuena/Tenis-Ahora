package com.tenisahora.modules.cancha.dto;

import java.time.LocalDate;
import java.util.List;

/** Grilla de una cancha: una franja de una hora por cada hora que se atiende, ordenada por fecha y hora. */
public record GrillaResponse(Long canchaId, LocalDate desde, LocalDate hasta, List<FranjaResponse> franjas) {
}
