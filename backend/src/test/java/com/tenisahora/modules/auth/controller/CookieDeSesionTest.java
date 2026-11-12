package com.tenisahora.modules.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookieDeSesionTest {

    @Test
    void laCookie_esHttpOnly_conRutaYVencimiento() {
        ResponseCookie cookie = new CookieDeSesion(true, "Strict").crear("valor-secreto", Duration.ofDays(7));

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("valor-secreto");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void enDesarrolloLaCookieNoEsSecure_perosigueSiendoHttpOnly() {
        ResponseCookie cookie = new CookieDeSesion(false, "Strict").crear("v", Duration.ofDays(7));

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void borrar_dejaLaCookieVencidaConElMismoNombreYRuta() {
        ResponseCookie cookie = new CookieDeSesion(true, "Strict").borrar();

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void sameSiteNone_exigeSecure() {
        assertThatThrownBy(() -> new CookieDeSesion(false, "None"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secure");
        assertThat(new CookieDeSesion(true, "None").crear("v", Duration.ofDays(1)).getSameSite()).isEqualTo("None");
    }

    @Test
    void unValorDeSameSiteInvalido_cortaElArranque() {
        assertThatThrownBy(() -> new CookieDeSesion(true, "Cualquiera")).isInstanceOf(IllegalStateException.class);
    }
}
