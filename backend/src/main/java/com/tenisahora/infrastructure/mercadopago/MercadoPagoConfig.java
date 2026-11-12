package com.tenisahora.infrastructure.mercadopago;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class MercadoPagoConfig {
}
