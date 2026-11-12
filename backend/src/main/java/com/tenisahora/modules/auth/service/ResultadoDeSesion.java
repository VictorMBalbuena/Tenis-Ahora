package com.tenisahora.modules.auth.service;

import java.time.Duration;

/**
 * Lo que devuelve el servicio al iniciar o renovar una sesión. El access token viaja en el cuerpo de la respuesta y el
 * refresh token va en una cookie httpOnly (la arma el controller), por eso el servicio los entrega por separado.
 */
public record ResultadoDeSesion(String accessToken, String refreshToken, Duration duracionDelRefresh) {
}
