package com.tenisahora.modules.actividad.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.actividad.dto.InscripcionResponse;
import com.tenisahora.modules.actividad.service.InscripcionesDeActividad;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Espejo de InscripcionesDeClaseControllerTest, sobre /api/entrenamientos (CU82). */
@WebMvcTest(InscripcionesDeEntrenamientoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class InscripcionesDeEntrenamientoControllerTest {

    private static final long ID_SOCIO = 3L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InscripcionesDeActividad inscripcionesDeActividad;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, "SOCIO");
    }

    @Test
    void inscribir_comoSocio_201() throws Exception {
        when(inscripcionesDeActividad.inscribir(eq(12L), any(), any(), any(), any())).thenReturn(
                new InscripcionResponse(45L, 12L, ID_SOCIO, "Apellido3, Nombre3", LocalDate.now(), null));

        mockMvc.perform(post("/api/entrenamientos/12/inscripciones").header("Authorization", socio()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/entrenamientos/12/inscripciones/45")));
    }

    @Test
    void inscribir_cupoCompleto_422() throws Exception {
        when(inscripcionesDeActividad.inscribir(eq(12L), any(), any(), any(), any())).thenThrow(
                new ReglaDeNegocioException(CodigoError.CUPO_COMPLETO, "La actividad ya no tiene cupo disponible"));

        mockMvc.perform(post("/api/entrenamientos/12/inscripciones").header("Authorization", socio()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CUPO_COMPLETO"));
    }
}
