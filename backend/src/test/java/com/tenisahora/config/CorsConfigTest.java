package com.tenisahora.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private CorsConfigurationSource fuenteCon(String origenes) {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "origenesPermitidos", origenes);
        return corsConfig.corsConfigurationSource();
    }

    private CorsConfiguration configuracionPara(CorsConfigurationSource fuente, String ruta) {
        return fuente.getCorsConfiguration(new MockHttpServletRequest("GET", ruta));
    }

    @Test
    void unOrigen_permiteSoloEseOrigen() {
        CorsConfiguration config = configuracionPara(fuenteCon("http://localhost:5173"), "/api/auth/login");

        assertThat(config.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(config.checkOrigin("http://sitio-malicioso.com")).isNull();
    }

    @Test
    void variosOrigenesSeparadosPorComa_permiteTodos() {
        CorsConfiguration config = configuracionPara(
                fuenteCon("http://localhost:5173, https://tenisahora.com"), "/api/auth/login");

        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173", "https://tenisahora.com");
    }

    @Test
    void permiteCredencialesParaLaCookieDelRefreshToken_soloConOrigenesConcretos() {
        CorsConfiguration config = configuracionPara(fuenteCon("http://localhost:5173"), "/api/auth/login");

        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    void exponeRetryAfterParaQueElFrontSepaCuantoEsperar() {
        CorsConfiguration config = configuracionPara(fuenteCon("http://localhost:5173"), "/api/auth/login");

        assertThat(config.getExposedHeaders()).containsExactly("Retry-After");
    }

    @Test
    void soloPermiteLosHeadersDeLaApi() {
        CorsConfiguration config = configuracionPara(fuenteCon("http://localhost:5173"), "/api/auth/login");

        assertThat(config.getAllowedHeaders()).containsExactlyInAnyOrder("Authorization", "Content-Type");
    }

    @Test
    void soloAplicaALasRutasDeLaApi() {
        assertThat(configuracionPara(fuenteCon("http://localhost:5173"), "/swagger-ui/index.html")).isNull();
    }

    @Test
    void comodin_cortaElArranque() {
        assertThatThrownBy(() -> fuenteCon("*")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> fuenteCon("http://localhost:5173,*")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinOrigenes_cortaElArranque() {
        assertThatThrownBy(() -> fuenteCon("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> fuenteCon(" , ")).isInstanceOf(IllegalStateException.class);
    }
}
