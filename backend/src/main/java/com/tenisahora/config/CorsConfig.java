package com.tenisahora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    // Puede venir más de un origen separado por comas (ej. el front local y el de producción)
    @Value("${app.cors.allowed-origins}")
    private String origenesPermitidos;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // La cookie del refresh token solo viaja si se permiten credenciales. Es seguro porque los orígenes son concretos
        // (origenesPermitidos corta el arranque si hay un "*"): un sitio ajeno no puede usarla.
        configuracion.setAllowCredentials(true);
        // Sin esto el JavaScript del front no puede leer cuánto esperar tras un 429
        configuracion.setExposedHeaders(List.of("Retry-After"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    private List<String> origenesPermitidos() {
        List<String> origenes = Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
        // Un "*" dejaría que cualquier página web llame a la API: se corta el arranque
        if (origenes.isEmpty() || origenes.contains("*")) {
            throw new IllegalStateException("app.cors.allowed-origins debe listar orígenes concretos, sin '*'");
        }
        return origenes;
    }
}
