package com.tenisahora.modules.actividad.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ActividadesProperties.class, InscripcionesProperties.class})
public class ActividadesConfig {
}
