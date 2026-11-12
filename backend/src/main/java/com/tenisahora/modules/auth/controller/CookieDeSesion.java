package com.tenisahora.modules.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Arma la cookie del refresh token: httpOnly (el JavaScript de la página no la puede leer, así un XSS no se la lleva),
 * limitada a /api/auth (solo viaja en login, refresh y logout) y con SameSite para frenar el CSRF.
 */
@Component
public class CookieDeSesion {

    public static final String NOMBRE = "refresh_token";
    private static final String RUTA = "/api/auth";
    private static final Set<String> SAME_SITE_VALIDOS = Set.of("Strict", "Lax", "None");

    private final boolean segura;
    private final String sameSite;

    public CookieDeSesion(@Value("${app.cookie.secure:false}") boolean segura,
                          @Value("${app.cookie.same-site:Strict}") String sameSite) {
        if (!SAME_SITE_VALIDOS.contains(sameSite)) {
            throw new IllegalStateException("app.cookie.same-site debe ser Strict, Lax o None");
        }
        if ("None".equals(sameSite) && !segura) {
            // Los navegadores descartan una cookie SameSite=None que no sea Secure
            throw new IllegalStateException("app.cookie.same-site=None exige app.cookie.secure=true");
        }
        this.segura = segura;
        this.sameSite = sameSite;
    }

    public ResponseCookie crear(String refreshToken, Duration duracion) {
        return base(refreshToken).maxAge(duracion).build();
    }

    /** Cookie vencida con el mismo nombre y ruta: el navegador la borra. */
    public ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite(sameSite)
                .path(RUTA);
    }
}
