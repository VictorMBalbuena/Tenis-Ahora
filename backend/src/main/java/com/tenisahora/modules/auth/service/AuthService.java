package com.tenisahora.modules.auth.service;

import com.tenisahora.modules.auth.dto.LoginRequest;

public interface AuthService {

    /** CU71: inicia sesión. Si la cuenta está bloqueada por intentos fallidos lanza DemasiadosIntentosException. */
    ResultadoDeSesion login(LoginRequest request);

    ResultadoDeSesion refresh(String refreshToken);

    void logout(String refreshToken);

    /** Revoca todos los refresh tokens de la persona (no solo el de esta pestaña): para cuando se perdió el teléfono. */
    void revocarTodasLasSesiones(Long personaId);
}
