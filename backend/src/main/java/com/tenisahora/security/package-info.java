/**
 * Seguridad: JwtService (genera y valida tokens), LimitadorDeIntentos (límite de pedidos por IP).
 * Las rutas públicas y los roles por ruta están en SecurityConfig, en config (no acá): ese paquete es el único
 * punto donde se arma la cadena de filtros de Spring Security.
 * Permisos por rol con @PreAuthorize en cada endpoint (activado con @EnableMethodSecurity): un rol que
 * no corresponde recibe 403 "No autorizado"; sin token o con token inválido, 401 "No autenticado".
 * VerificadorDeSesion: puerto con el que el filtro JWT comprueba en cada pedido que la cuenta sigue activa y toma
 * el rol vigente de la base; lo implementa el módulo auth.
 * No depende de ningún módulo: para buscar al usuario usa la interfaz UserDetailsService de Spring,
 * que la implementa el módulo auth.
 */
package com.tenisahora.security;
