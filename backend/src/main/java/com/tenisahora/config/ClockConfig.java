package com.tenisahora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Reloj único del sistema, con la zona del club. Se inyecta para poder probar las reglas de tiempo con un reloj falso. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }
}
