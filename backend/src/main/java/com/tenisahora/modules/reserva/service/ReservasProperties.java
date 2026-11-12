package com.tenisahora.modules.reserva.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Reglas de las reservas que el club puede ajustar sin tocar el código (application.yml, app.reservas). Se validan al arrancar. */
@Validated
@ConfigurationProperties(prefix = "app.reservas")
public record ReservasProperties(
        @DefaultValue("30") @Min(1) int antelacionMaximaDias,
        @DefaultValue("2") @Min(1) @Max(12) int duracionMaximaHoras,
        @DefaultValue("6") @Min(0) int plazoCancelacionHoras,
        @DefaultValue("50") @Min(1) @Max(100) int porcentajeSenia,
        @DefaultValue("100") @Min(0) @Max(100) int porcentajeReembolsoATiempo,
        @DefaultValue("30") @Min(1) int minutosParaVencerPendientes,
        @DefaultValue("30") @Min(0) int minutosAntesParaIniciar,
        @DefaultValue("30") @Min(0) int margenFinalizacionMinutos,
        @DefaultValue("10") @Min(1) int maxRenglonesDeInsumos,
        @DefaultValue("20") @Min(1) int maxUnidadesPorInsumo
) {
}
