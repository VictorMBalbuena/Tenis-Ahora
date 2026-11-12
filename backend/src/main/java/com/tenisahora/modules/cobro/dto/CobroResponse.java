package com.tenisahora.modules.cobro.dto;

import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un cobro. El {@code qrData} solo viene completo en la respuesta de la operación que generó el QR (registrar con
 * medio QR, o {@code POST /api/cobros/{id}/qr}): no se persiste, así que una consulta posterior no lo repite.
 * El {@code recibo} solo viene al consultar un cobro puntual; es null si todavía no se emitió.
 */
public record CobroResponse(
        Long id,
        @Schema(example = "C-000123") String codigo,
        Long reservaId,
        @Schema(example = "Inscripción a Tenis inicial") String origen,
        ConceptoCobro concepto,
        EstadoCobro estado,
        MedioPago medioPago,
        Integer cuotas,
        BigDecimal montoBase,
        BigDecimal montoFinal,
        LocalDateTime fechaExpiracionQr,
        String numeroOperacion,
        LocalDateTime fecha,
        String qrData,
        String motivoAnulacion,
        LocalDateTime fechaAnulacion,
        Long anuladoPorId,
        ReciboResponse recibo
) {
}
