package com.tenisahora.infrastructure.mercadopago;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Datos de la pasarela (application.yml, app.mercadopago). Las credenciales llegan por variable de entorno y
 * nunca están en el repositorio.
 *
 * @param accessToken       credencial de la cuenta vendedora de prueba (MP_ACCESS_TOKEN); vacío en local si no se usa
 * @param webhookSecret     clave con la que se verifica la firma de los avisos (MP_WEBHOOK_SECRET)
 * @param urlBase           dirección de la API. Es fija por configuración: nunca se toma del aviso recibido
 * @param minutosVigenciaQr cuánto dura el QR antes de vencer
 * @param sandbox           true con credenciales de prueba: el link normal no deja pagar a un comprador de prueba
 */
@Validated
@ConfigurationProperties(prefix = "app.mercadopago")
public record MercadoPagoProperties(
        @DefaultValue("") String accessToken,
        @DefaultValue("") String webhookSecret,
        @DefaultValue("https://api.mercadopago.com") String urlBase,
        @DefaultValue("10") @Min(1) @Max(60) int minutosVigenciaQr,
        @DefaultValue("false") boolean sandbox
) {

    /** Sin credencial no se puede pedir un QR: se avisa al usarla, no al arrancar, para no romper el entorno local. */
    public boolean estaConfigurada() {
        return !accessToken.isBlank();
    }

    public boolean verificaFirmas() {
        return !webhookSecret.isBlank();
    }
}
