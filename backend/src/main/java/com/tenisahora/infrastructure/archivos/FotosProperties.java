package com.tenisahora.infrastructure.archivos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Dónde quedan las fotos de perfil en el disco del servidor (application.yml, app.fotos). */
@Validated
@ConfigurationProperties(prefix = "app.fotos")
public record FotosProperties(
        @DefaultValue("./uploads/fotos") String directorio
) {
}
