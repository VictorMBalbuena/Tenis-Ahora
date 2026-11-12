package com.tenisahora.modules.descuento.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.reserva.service.DescuentoPorCodigo;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validar un código de descuento al reservar (RF69).
 *
 * Lo que importa acá es quién puede: el ABM de descuentos es solo del administrador, pero esta ruta la tiene
 * que poder usar el socio, que es el que reserva. Si no, el código no le sirve de nada.
 */
@WebMvcTest(CodigoDeDescuentoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class CodigoDeDescuentoControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DescuentoPorCodigo descuentoPorCodigo;

    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;

    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_ADMIN), any())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), any())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), any())).thenReturn(Optional.of("SOCIO"));
    }

    private String token(long id, Rol rol) {
        return "Bearer " + jwtService.generarToken(id, rol.name());
    }

    private void cyberweekDescuenta25() {
        when(descuentoPorCodigo.aplicar(any(), any())).thenReturn(new DescuentoPorCodigo.DescuentoAplicado(
                "CYBERWEEK", "25% de descuento", new BigDecimal("4000.00"), new BigDecimal("12000.00")));
    }

    @Test
    @DisplayName("El socio puede validar su código: es el que reserva")
    void socio200() throws Exception {
        cyberweekDescuenta25();

        mockMvc.perform(get("/api/descuentos/codigo/cyberweek?monto=16000")
                        .header("Authorization", token(ID_SOCIO, Rol.SOCIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CYBERWEEK"))
                .andExpect(jsonPath("$.descripcion").value("25% de descuento"))
                .andExpect(jsonPath("$.montoDescontado").value(4000.00))
                .andExpect(jsonPath("$.montoFinal").value(12000.00));
    }

    @Test
    @DisplayName("El empleado también, porque reserva para un socio")
    void empleado200() throws Exception {
        cyberweekDescuenta25();

        mockMvc.perform(get("/api/descuentos/codigo/CYBERWEEK?monto=16000")
                        .header("Authorization", token(ID_EMPLEADO, Rol.EMPLEADO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El administrador también")
    void admin200() throws Exception {
        cyberweekDescuenta25();

        mockMvc.perform(get("/api/descuentos/codigo/CYBERWEEK?monto=16000")
                        .header("Authorization", token(ID_ADMIN, Rol.ADMINISTRADOR)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin sesión no se puede probar códigos")
    void sinToken401() throws Exception {
        mockMvc.perform(get("/api/descuentos/codigo/CYBERWEEK?monto=16000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un código que no existe devuelve 404 con el mensaje de la API")
    void codigoInexistente404() throws Exception {
        when(descuentoPorCodigo.aplicar(any(), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.NO_ENCONTRADO, "El código de descuento no existe o no está vigente"));

        mockMvc.perform(get("/api/descuentos/codigo/NOEXISTE?monto=16000")
                        .header("Authorization", token(ID_SOCIO, Rol.SOCIO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_ENCONTRADO.name()));
    }

    @Test
    @DisplayName("Sin monto se puede consultar igual: sirve para saber si el código existe")
    void sinMonto200() throws Exception {
        cyberweekDescuenta25();

        mockMvc.perform(get("/api/descuentos/codigo/CYBERWEEK")
                        .header("Authorization", token(ID_SOCIO, Rol.SOCIO)))
                .andExpect(status().isOk());
    }
}
