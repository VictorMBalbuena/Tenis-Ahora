package com.tenisahora.security;

import com.tenisahora.config.SecurityConfig;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.config.CorsConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio de aceptación de BACK 02: "endpoint con un rol que no corresponde → 403".
 * Todavía no hay endpoints con permisos por rol, así que se prueba con un controller que solo existe en el test.
 */
@WebMvcTest(PermisosPorRolTest.ControllerDePrueba.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class,
        GlobalExceptionHandler.class, PermisosPorRolTest.ControllerDePrueba.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class PermisosPorRolTest {

    @RestController
    static class ControllerDePrueba {

        @GetMapping("/api/prueba/solo-admin")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        String soloAdmin() {
            return "ok";
        }

        @GetMapping("/api/prueba/empleado-o-admin")
        @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")
        String empleadoOAdmin() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        // Cada rol tiene su propio id y la "base" devuelve su rol vigente
        for (Rol rol : Rol.values()) {
            when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(idDe(rol)), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(rol.name()));
        }
    }

    private Long idDe(Rol rol) {
        return (long) rol.ordinal() + 1;
    }

    private String bearer(Rol rol) {
        Socio persona = new Socio();
        persona.setId(idDe(rol));
        return "Bearer " + jwtService.generarToken(persona.getId(), rol.name());
    }

    @Test
    void soloAdmin_conAdministrador_200() throws Exception {
        mockMvc.perform(get("/api/prueba/solo-admin").header("Authorization", bearer(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk());
    }

    @Test
    void soloAdmin_conSocio_403() throws Exception {
        mockMvc.perform(get("/api/prueba/solo-admin").header("Authorization", bearer(Rol.SOCIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No autorizado"));
    }

    @Test
    void soloAdmin_conEmpleado_403() throws Exception {
        mockMvc.perform(get("/api/prueba/solo-admin").header("Authorization", bearer(Rol.EMPLEADO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAdmin_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/prueba/solo-admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("No autenticado"));
    }

    @Test
    void empleadoOAdmin_conEmpleado_200() throws Exception {
        mockMvc.perform(get("/api/prueba/empleado-o-admin").header("Authorization", bearer(Rol.EMPLEADO)))
                .andExpect(status().isOk());
    }

    @Test
    void empleadoOAdmin_conAdministrador_200() throws Exception {
        mockMvc.perform(get("/api/prueba/empleado-o-admin").header("Authorization", bearer(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk());
    }

    @Test
    void empleadoOAdmin_conProfesor_403() throws Exception {
        mockMvc.perform(get("/api/prueba/empleado-o-admin").header("Authorization", bearer(Rol.PROFESOR)))
                .andExpect(status().isForbidden());
    }
}
