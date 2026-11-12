package com.tenisahora.modules.actividad.service;

import java.math.BigDecimal;

/** Avisa que se dio de baja una inscripción y cuánto corresponde devolver. */
public record InscripcionCancelada(Long inscripcionId, BigDecimal montoReembolso, Long responsableId) {
}
