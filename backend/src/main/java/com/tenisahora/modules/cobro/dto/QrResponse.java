package com.tenisahora.modules.cobro.dto;

import com.tenisahora.modules.cobro.entity.EstadoCobro;

import java.time.LocalDateTime;

/** El QR para pagar un cobro (CU77, CU88). */
public record QrResponse(String qrData, LocalDateTime venceEn, EstadoCobro estado) {
}
