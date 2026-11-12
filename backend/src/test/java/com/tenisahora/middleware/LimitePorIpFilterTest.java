package com.tenisahora.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.support.RelojFalso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LimitePorIpFilterTest {

    private RelojFalso reloj;
    private LimitePorIpFilter filtro;

    @BeforeEach
    void setUp() {
        reloj = new RelojFalso(Instant.parse("2026-10-10T12:00:00Z"));
        // Topes chicos para probar: login 3 por 15 min, registro 2 por 60 min, refresh 5 por 15 min
        filtro = new LimitePorIpFilter(new LimitadorDeIntentos(reloj), new ObjectMapper().findAndRegisterModules(),
                3, 15, 2, 60, 5, 15);
    }

    private MockHttpServletResponse pedir(String metodo, String ruta, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private int estadoDe(String metodo, String ruta, String ip) throws Exception {
        return pedir(metodo, ruta, ip).getStatus();
    }

    @Test
    void permiteHastaElMaximoYDespuesResponde429() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(estadoDe("POST", "/api/auth/login", "10.0.0.1")).isEqualTo(200);
        }

        assertThat(estadoDe("POST", "/api/auth/login", "10.0.0.1")).isEqualTo(429);
    }

    @Test
    void el429_llevaRetryAfterYElErrorEnElFormatoDeLaApi() throws Exception {
        for (int i = 0; i < 2; i++) {
            pedir("POST", "/api/auth/register", "10.0.0.1");
        }

        MockHttpServletResponse response = pedir("POST", "/api/auth/register", "10.0.0.1");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("3600");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"codigo\":\"LIMITE_DE_PEDIDOS_SUPERADO\"")
                .contains("60 minuto")
                .contains("\"ruta\":\"/api/auth/register\"");
    }

    @Test
    void elPedidoBloqueado_noLlegaAlController() throws Exception {
        for (int i = 0; i < 3; i++) {
            pedir("POST", "/api/auth/login", "10.0.0.1");
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        assertThat(cadena.getRequest()).isNull();
    }

    @Test
    void cadaIpTieneSuPropioLimite() throws Exception {
        for (int i = 0; i < 3; i++) {
            pedir("POST", "/api/auth/login", "10.0.0.1");
        }

        assertThat(estadoDe("POST", "/api/auth/login", "10.0.0.2")).isEqualTo(200);
    }

    @Test
    void cadaEndpointTieneSuPropioContador_yElRegistroEsMasEstricto() throws Exception {
        for (int i = 0; i < 2; i++) {
            pedir("POST", "/api/auth/register", "10.0.0.1");
        }

        assertThat(estadoDe("POST", "/api/auth/register", "10.0.0.1")).isEqualTo(429);
        assertThat(estadoDe("POST", "/api/auth/login", "10.0.0.1")).isEqualTo(200);
    }

    @Test
    void conElTiempoSeLiberaElLimite() throws Exception {
        for (int i = 0; i < 3; i++) {
            pedir("POST", "/api/auth/login", "10.0.0.1");
        }
        assertThat(estadoDe("POST", "/api/auth/login", "10.0.0.1")).isEqualTo(429);

        reloj.avanzar(Duration.ofMinutes(15));

        assertThat(estadoDe("POST", "/api/auth/login", "10.0.0.1")).isEqualTo(200);
    }

    @Test
    void soloControlaLosEndpointsPublicosYSoloPOST() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(estadoDe("GET", "/api/auth/login", "10.0.0.1")).isEqualTo(200);
            assertThat(estadoDe("POST", "/api/personas", "10.0.0.1")).isEqualTo(200);
            assertThat(estadoDe("POST", "/api/auth/logout", "10.0.0.1")).isEqualTo(200);
        }
    }

    @Test
    void tambienLimitaLaRenovacion() throws Exception {
        for (int i = 0; i < 5; i++) {
            pedir("POST", "/api/auth/refresh", "10.0.0.1");
        }

        assertThat(estadoDe("POST", "/api/auth/refresh", "10.0.0.1")).isEqualTo(429);
    }
}
