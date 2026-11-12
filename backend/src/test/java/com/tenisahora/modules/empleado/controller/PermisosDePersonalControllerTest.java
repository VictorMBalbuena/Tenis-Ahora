package com.tenisahora.modules.empleado.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.administrador.controller.AdministradorController;
import com.tenisahora.modules.administrador.service.AdministradorService;
import com.tenisahora.modules.empleado.service.EmpleadoService;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quién entra a /api/empleados y a /api/administradores. La lógica de cada operación se prueba en los servicios. */
@WebMvcTest({EmpleadoController.class, AdministradorController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class PermisosDePersonalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EmpleadoService empleadoService;
    @MockitoBean
    private AdministradorService administradorService;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        for (Rol rol : Rol.values()) {
            when(verificadorDeSesion.rolVigente(eq(idDe(rol)), anyString())).thenReturn(Optional.of(rol.name()));
        }
    }

    private long idDe(Rol rol) {
        return rol.ordinal() + 1L;
    }

    private String bearer(Rol rol) {
        return "Bearer " + jwtService.generarToken(idDe(rol), rol.name());
    }

    private PersonaResponse respuesta(Rol rol) {
        return new PersonaResponse(12L, "Ana", "Gómez", "30123456", null, null, "Calle", "1", "11", "12345678",
                "ana@correo.com", rol, true, LocalDate.of(2026, 10, 10));
    }

    @Test
    void empleados_losRolesSinPermiso_403_enTodosLosEndpoints() throws Exception {
        for (Rol rol : new Rol[]{Rol.SOCIO, Rol.PROFESOR, Rol.ENTRENADOR}) {
            String token = bearer(rol);
            mockMvc.perform(get("/api/empleados").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/empleados/12").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/empleados").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/empleados/12").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/empleados/12").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/empleados/12/pdf").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void empleados_unEmpleadoSiEntra() throws Exception {
        when(empleadoService.consultar(12L)).thenReturn(respuesta(Rol.EMPLEADO));

        mockMvc.perform(get("/api/empleados/12").header("Authorization", bearer(Rol.EMPLEADO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("EMPLEADO"));
    }

    @Test
    void administradores_todosMenosElAdministrador_403_enTodosLosEndpoints() throws Exception {
        for (Rol rol : new Rol[]{Rol.SOCIO, Rol.EMPLEADO, Rol.PROFESOR, Rol.ENTRENADOR}) {
            String token = bearer(rol);
            mockMvc.perform(get("/api/administradores").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/administradores/1").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/administradores").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/administradores/1").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/administradores/1").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/administradores/1/pdf").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void administradores_unAdministradorSiEntra() throws Exception {
        when(administradorService.consultar(1L)).thenReturn(respuesta(Rol.ADMINISTRADOR));

        mockMvc.perform(get("/api/administradores/1").header("Authorization", bearer(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/api/administradores/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/empleados/1")).andExpect(status().isUnauthorized());
    }
}
