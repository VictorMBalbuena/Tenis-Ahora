package com.tenisahora.modules.actividad.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Plazos del cobro de inscripciones a clases y entrenamientos, ajustables sin tocar el código (app.inscripciones). */
@Validated
@ConfigurationProperties(prefix = "app.inscripciones")
public record InscripcionesProperties(
        @DefaultValue("30") @Min(1) int minutosParaVencer,
        @DefaultValue("6") @Min(0) int plazoCancelacionHoras,
        @DefaultValue("100") @Min(0) @Max(100) int porcentajeReembolsoATiempo
) {
}
