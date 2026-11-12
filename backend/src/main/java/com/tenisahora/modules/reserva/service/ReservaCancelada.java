package com.tenisahora.modules.reserva.service;

import java.math.BigDecimal;

/** Avisa que se canceló una reserva y cuánto corresponde devolver. Nulo el responsable si la canceló el sistema. */
public record ReservaCancelada(Long reservaId, BigDecimal montoReembolso, Long responsableId) {
}
