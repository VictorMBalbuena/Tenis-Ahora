package com.tenisahora.modules.actividad.service;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Reglas de las clases que el club puede ajustar sin tocar el código (application.yml, app.actividades). RD01, RD04. */
@Validated
@ConfigurationProperties(prefix = "app.actividades")
public record ActividadesProperties(
        @DefaultValue("30") @Min(1) int cupoMaximoGeneral,
        @DefaultValue("30") @Min(1) int cupoPorDefecto
) {
}
