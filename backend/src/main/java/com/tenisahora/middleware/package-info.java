/**
 * MIDDLEWARE: código que se ejecuta en cada request, antes o después del controller.
 * JwtAuthenticationFilter (lee el token y autentica al usuario), RequestLoggingFilter (registra
 * método, ruta, estado y tiempo, sin datos sensibles), LoginRateLimitFilter (bloquea temporalmente
 * tras varios logins fallidos, OWASP A07).
 * En Spring se implementan como Filter (OncePerRequestFilter) o HandlerInterceptor.
 */
package com.tenisahora.middleware;
