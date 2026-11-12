package com.tenisahora.modules.cobro.service;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Reglas de los cobros que el club puede ajustar sin tocar el código (application.yml, app.cobros). */
@Validated
@ConfigurationProperties(prefix = "app.cobros")
public record CobrosProperties(
        @DefaultValue("24") @Min(1) int maxCuotas,
        @DefaultValue("60000") @Min(1000) long intervaloVencimientoQrMs,
        @DefaultValue("300000") @Min(1000) long intervaloConciliacionMs
) {
}
