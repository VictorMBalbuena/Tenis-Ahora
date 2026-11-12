package com.tenisahora.modules.reserva.dto;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ReservaResponse(
        @Schema(example = "123") Long id,
        @Schema(description = "Número de reserva", example = "R-000123") String numero,
        Long canchaId,
        @Schema(example = "Cancha 1") String cancha,
        LocalDate fecha,
        @Schema(example = "18:00:00", type = "string") LocalTime horaInicio,
        @Schema(example = "20:00:00", type = "string") LocalTime horaFin,
        EstadoReserva estado,
        Integer cantidadJugadores,
        List<JugadorResponse> jugadores,
        @Schema(description = "Total ya con el descuento restado, si se aplicó alguno", example = "12000.00") BigDecimal montoTotal,
        @Schema(description = "Código de descuento aplicado, si hubo", example = "CYBERWEEK") String codigoDescuento,
        @Schema(description = "Cuánto se descontó del total por ese código", example = "4000.00") BigDecimal montoDescuento,
        @Schema(description = "La seña que hay que pagar para confirmarla", example = "8000.00") BigDecimal montoSenia,
        @Schema(description = "Hasta cuándo se espera el pago de la seña; vacío si ya no está pendiente") LocalDateTime venceEn,
        LocalDateTime fechaCancelacion,
        @Schema(description = "Cargo por cancelar fuera de plazo") BigDecimal montoPenalidad,
        @Schema(description = "Lo que corresponde devolver al cancelar a tiempo") BigDecimal montoReembolso,
        List<InsumoAsignadoResponse> insumos,
        @Schema(description = "Cuándo se registró el inicio del alquiler") LocalDateTime inicioReal,
        @Schema(description = "Cuándo se registró su finalización") LocalDateTime finReal,
        @Schema(description = "Lo que entró en cobros pagados") BigDecimal montoPagado,
        @Schema(description = "Lo que falta cobrar") BigDecimal saldoPendiente,
        @Schema(description = "Lo cobrado de más si el total bajó al modificar") BigDecimal saldoAFavor
) {
}
