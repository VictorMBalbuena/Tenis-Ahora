package com.tenisahora.modules.descuento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Lo que se le contesta a quien valida un código al reservar.
 *
 * <p>A propósito no devuelve el descuento entero: quien reserva no tiene por qué ver el id, las condiciones
 * internas ni cuántas veces se usó. Solo necesita saber que el código sirve y cuánto le queda a pagar.
 */
@Schema(description = "Resultado de validar un código de descuento sobre un monto")
public record CodigoValidadoResponse(
        @Schema(example = "CYBERWEEK") String codigo,
        @Schema(description = "Cómo se le explica el beneficio a la persona", example = "25% de descuento") String descripcion,
        @Schema(description = "El monto sobre el que se calculó", example = "16000.00") BigDecimal montoBase,
        @Schema(description = "Cuánto se descuenta", example = "4000.00") BigDecimal montoDescontado,
        @Schema(description = "Lo que queda a pagar", example = "12000.00") BigDecimal montoFinal) {
}
