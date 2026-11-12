package com.tenisahora.modules.cobro.dto;

import com.tenisahora.modules.cobro.entity.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** CU51: cobro de la seña de una reserva. El monto nunca viaja acá: lo calcula siempre el servidor (RD09, A04). */
public record CobroRequest(
        @Schema(description = "Medio con el que se cobra (RD23)") @NotNull(message = "Indicá el medio de pago") MedioPago medioPago,
        @Schema(description = "Número que devuelve la terminal, para débito y crédito", example = "000123")
        @Pattern(regexp = "\\d{1,20}", message = "El número de operación tiene que tener solo números, hasta 20 dígitos") String numeroOperacion,
        @Schema(description = "Cantidad de cuotas, solo para crédito", example = "1") @Min(1) @Max(24) Integer cuotas,
        @Schema(description = "Si la terminal aprobó la operación (débito o crédito). Por defecto, aprobada", defaultValue = "true")
        Boolean aprobado
) {

    public CobroRequest {
        if (aprobado == null) {
            aprobado = true;
        }
    }

    @AssertTrue(message = "Las cuotas solo se pueden indicar con crédito")
    public boolean isCuotasConCredito() {
        return cuotas == null || medioPago == MedioPago.CREDITO;
    }
}
