/**
 * Configuración de Spring. No depende de ningún módulo.
 * SecurityConfig (rutas públicas y roles), CorsConfig (orígenes del frontend), OpenApiConfig (Swagger),
 * SchedulingConfig (@EnableScheduling), AsyncConfig (@EnableAsync, el executor del webhook de Mercado Pago),
 * ClockConfig (reloj con zona America/Argentina/Buenos_Aires: Render usa UTC, y sin esto las reglas
 * de 6 horas y 30 días se calcularían con 3 horas de diferencia), DataSeeder (datos iniciales de desarrollo).
 */
package com.tenisahora.config;
