package com.tenisahora.modules.cobro.dto;

import com.tenisahora.modules.cobro.entity.EstadoRecibo;
import com.tenisahora.modules.cobro.entity.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** El comprobante de un cobro pagado (CU57). */
public record ReciboResponse(Long id, String numeroRecibo, EstadoRecibo estado, BigDecimal monto, String concepto,
                             MedioPago medioPago, String numeroOperacion, String observaciones, LocalDateTime fechaEmision,
                             CobroDeRecibo cobro) {

    public record CobroDeRecibo(Long id, String codigo, BigDecimal montoFinal, LocalDateTime fecha) {
    }
}
