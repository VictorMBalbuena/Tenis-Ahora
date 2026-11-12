package com.tenisahora.modules.auth.controller;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.config.CorsConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.auth.dto.LoginRequest;
import com.tenisahora.modules.auth.service.AuthService;
import com.tenisahora.modules.auth.service.ResultadoDeSesion;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.DemasiadosIntentosException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class,
        GlobalExceptionHandler.class, CookieDeSesion.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.cookie.secure=true",
        "app.cookie.same-site=Strict"
})
class AuthControllerTest {

    private static final String LOGIN_OK = """
            {"email":"socio@tenisahora.com","password":"TenisAhora2026!"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;

    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    private ResultadoDeSesion sesion(String access, String refresh) {
        return new ResultadoDeSesion(access, refresh, Duration.ofDays(7));
    }

    // ---------- POST /api/auth/login (CU71) ----------

    @Test
    void login_credencialesCorrectas_200ConElAccessTokenEnElCuerpo() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sesion("access", "refresh-secreto"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void login_elRefreshTokenNoVaEnElCuerpo_vaEnUnaCookieHttpOnly() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sesion("access", "refresh-secreto"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token=refresh-secreto"),
                        containsString("HttpOnly"),
                        containsString("Secure"),
                        containsString("SameSite=Strict"),
                        containsString("Path=/api/auth"),
                        containsString("Max-Age=604800"))));
    }

    @Test
    void login_credencialesIncorrectas_401ConMensajeGeneralYSinCookie() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales invalidas"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_cuentaDeshabilitada_401ConElMismoMensaje() throws Exception {
        // CU71 A2: no debe revelar si la cuenta existe o está deshabilitada
        when(authService.login(any(LoginRequest.class))).thenThrow(new DisabledException("User is disabled"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales invalidas"));
    }

    @Test
    void login_cuentaBloqueadaPorIntentosFallidos_429ConRetryAfter() throws Exception {
        // CU71 A3
        when(authService.login(any(LoginRequest.class))).thenThrow(new DemasiadosIntentosException(
                CodigoError.LOGIN_BLOQUEADO, "Demasiados intentos fallidos. Esperá 15 minuto(s) antes de volver a intentar.",
                Duration.ofMinutes(15)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.codigo").value("LOGIN_BLOQUEADO"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_emailMalFormado_400SinLlamarAlService() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"socio\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
        verify(authService, never()).login(any());
    }

    @Test
    void login_camposVacios_400SinLlamarAlService() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(authService, never()).login(any());
    }

    @Test
    void login_esPublico_noPideToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sesion("access", "refresh"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(status().isOk());
    }

    @Test
    void login_superaElLimitePorIp_429ConCorsYSinLlegarAlController() throws Exception {
        when(limitadorDeIntentos.intentar(anyString(), anyInt(), any())).thenReturn(Optional.of(Duration.ofSeconds(90)));

        mockMvc.perform(post("/api/auth/login").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "90"))
                .andExpect(jsonPath("$.codigo").value("LIMITE_DE_PEDIDOS_SUPERADO"))
                // El 429 lleva los encabezados de CORS: así el front puede leer el mensaje y el Retry-After
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        verify(authService, never()).login(any());
    }

    // ---------- POST /api/auth/refresh ----------

    @Test
    void refresh_conLaCookie_200ConAccessNuevoYCookieRotada() throws Exception {
        when(authService.refresh("rt-1")).thenReturn(sesion("access-2", "rt-2"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "rt-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-2"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=rt-2")));
    }

    @Test
    void refresh_sinCookie_401YNoLlamaAlService() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        verify(authService, never()).refresh(any());
    }

    @Test
    void refresh_conUnaCookieInvalida_401() throws Exception {
        when(authService.refresh("rt-viejo")).thenThrow(new BadCredentialsException("Refresh token invalido o vencido"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "rt-viejo"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales invalidas"));
    }

    @Test
    void refresh_unPedidoQueNoEsJson_415_asiUnFormularioDeOtroSitioNoLoDispara() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "rt-1"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).content("a=b"))
                .andExpect(status().isUnsupportedMediaType());
        verify(authService, never()).refresh(any());
    }

    // ---------- POST /api/auth/logout (CU72) ----------

    @Test
    void logout_conLaCookie_204RevocaYBorraLaCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", "rt-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token="),
                        containsString("Max-Age=0"),
                        containsString("Path=/api/auth"))));
        verify(authService).logout("rt-1");
    }

    @Test
    void logout_sinCookie_204IgualYNoLlamaAlService() throws Exception {
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        verify(authService, never()).logout(any());
    }

    @Test
    void logout_unPedidoQueNoEsJson_415() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", "rt-1"))
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        verify(authService, never()).logout(any());
    }

    // ---------- Rutas protegidas ----------

    @Test
    void rutaProtegida_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/canchas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("No autenticado"));
    }

    @Test
    void rutaProtegida_tokenInvalido_401() throws Exception {
        mockMvc.perform(get("/api/canchas").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegida_tokenValido_pasaLaSeguridad() throws Exception {
        Socio persona = new Socio();
        persona.setId(1L);
        String token = jwtService.generarToken(persona.getId(), "ADMINISTRADOR");
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ADMINISTRADOR"));

        // No hay controller de canchas todavía: 404 significa que la seguridad lo dejó pasar
        mockMvc.perform(get("/api/canchas").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void rutaProtegida_tokenDePersonaEliminadaODeshabilitada_401() throws Exception {
        Socio persona = new Socio();
        persona.setId(1L);
        String token = jwtService.generarToken(persona.getId(), "ADMINISTRADOR");
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/canchas").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- CORS ----------

    @Test
    void cors_origenDelFront_permitidoConCredenciales() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void cors_otroOrigen_rechazado() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://sitio-malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void cors_retryAfterQuedaExpuestoParaElFront() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new DemasiadosIntentosException(
                CodigoError.LOGIN_BLOQUEADO, "Bloqueada", Duration.ofMinutes(15)));

        mockMvc.perform(post("/api/auth/login").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_OK))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")));
    }
}
