package com.tenisahora.modules.socio.controller;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.config.CorsConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.socio.dto.RegistroSocioRequest;
import com.tenisahora.modules.socio.dto.SocioResponse;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.service.SocioService;
import com.tenisahora.security.JwtService;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistroController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class RegistroControllerTest {

    private static final String REGISTRO_VALIDO = """
            {"nombre":"Ana","apellido":"Gómez","dni":"30123456","calle":"Av. Siempreviva",
             "numeroCalle":"742","codigoArea":"11","numeroTelefono":"12345678","email":"ana@correo.com","password":"Clave-segura-123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocioService socioService;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    private SocioResponse socio() {
        return new SocioResponse(12L, "Ana", "Gómez", "30123456", null, null, "Av. Siempreviva",
                "742", "11", "12345678", "ana@correo.com", Rol.SOCIO, true, LocalDate.of(2026, 10, 10), "S-0012");
    }

    @Test
    void registrarse_esPublico_201ConRolSocio() throws Exception {
        when(socioService.registrarse(any(RegistroSocioRequest.class))).thenReturn(socio());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.rol").value("SOCIO"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrarse_unRolEnviadoEnElPedidoSeIgnora() throws Exception {
        when(socioService.registrarse(any(RegistroSocioRequest.class))).thenReturn(socio());
        String conRol = REGISTRO_VALIDO.replace("\"password\"", "\"rol\":\"ADMINISTRADOR\",\"activo\":false,\"password\"");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(conRol))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("SOCIO"));

        org.assertj.core.api.Assertions.assertThat(
                java.util.Arrays.stream(RegistroSocioRequest.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("rol", "activo", "id");
    }

    @Test
    void registrarse_telefonoYCalleConFormatoInvalido_400PorCadaCampo() throws Exception {
        String formatoInvalido = REGISTRO_VALIDO
                .replace("\"numeroCalle\":\"742\"", "\"numeroCalle\":\"7a2\"")
                .replace("\"codigoArea\":\"11\"", "\"codigoArea\":\"011\\u0031\\u0031\"")
                .replace("\"numeroTelefono\":\"12345678\"", "\"numeroTelefono\":\"15-1234\"");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(formatoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.numeroCalle").exists())
                .andExpect(jsonPath("$.campos.codigoArea").exists())
                .andExpect(jsonPath("$.campos.numeroTelefono").exists());
        verify(socioService, never()).registrarse(any());
    }

    @Test
    void registrarse_sinDatos_400ConElDetalleDeCadaCampo() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.apellido").exists())
                .andExpect(jsonPath("$.campos.dni").exists())
                .andExpect(jsonPath("$.campos.calle").exists())
                .andExpect(jsonPath("$.campos.numeroCalle").exists())
                .andExpect(jsonPath("$.campos.codigoArea").exists())
                .andExpect(jsonPath("$.campos.numeroTelefono").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.password").exists());
        verify(socioService, never()).registrarse(any());
    }

    @Test
    void registrarse_emailConFormatoInvalido_400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO.replace("ana@correo.com", "ana-correo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").value("El correo electrónico no tiene un formato válido"));
    }

    @Test
    void registrarse_contraseniaDeMenosDe8Caracteres_400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO.replace("Clave-segura-123", "1234567")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void registrarse_contraseniaDeMasDe72Caracteres_400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO.replace("Clave-segura-123", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void registrarse_dniQueNoEsNumerico_400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO.replace("30123456", "30.123.456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dni").exists());
    }

    @Test
    void registrarse_dniORegistrado_409() throws Exception {
        when(socioService.registrarse(any(RegistroSocioRequest.class)))
                .thenThrow(new ConflictoException(CodigoError.PERSONA_EMAIL_DUPLICADO, "Ya existe una persona con ese correo electrónico"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERSONA_EMAIL_DUPLICADO"));
    }

    @Test
    void registrarse_conUnJsonRoto_400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
    }
}
