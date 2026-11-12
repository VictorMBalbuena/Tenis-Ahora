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
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InscripcionesDeClaseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class InscripcionesDeClaseControllerTest {

    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InscripcionesDeActividad inscripcionesDeClase;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(eq(ID_PROFESOR), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("PROFESOR"));
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, "SOCIO");
    }

    private String empleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, "EMPLEADO");
    }

    private String profesor() {
        return "Bearer " + jwtService.generarToken(ID_PROFESOR, "PROFESOR");
    }

    @Test
    void inscribir_conRolProfesor_403() throws Exception {
        mockMvc.perform(post("/api/clases/12/inscripciones").header("Authorization", profesor())).andExpect(status().isForbidden());
    }

    @Test
    void inscribir_comoSocio_201() throws Exception {
        when(inscripcionesDeClase.inscribir(eq(12L), any(), any(), any(), any())).thenReturn(
                new InscripcionResponse(45L, 12L, ID_SOCIO, "Apellido3, Nombre3", LocalDate.now(), null));

        mockMvc.perform(post("/api/clases/12/inscripciones").header("Authorization", socio()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/clases/12/inscripciones/45")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")));
    }

    @Test
    void inscribir_cupoCompleto_422_CUPO_COMPLETO() throws Exception {
        when(inscripcionesDeClase.inscribir(eq(12L), any(), any(), any(), any())).thenThrow(
                new ReglaDeNegocioException(CodigoError.CUPO_COMPLETO, "La clase ya no tiene cupo disponible"));

        mockMvc.perform(post("/api/clases/12/inscripciones").header("Authorization", socio()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CUPO_COMPLETO"));
    }

    @Test
    void inscribir_yaInscripto_409() throws Exception {
        when(inscripcionesDeClase.inscribir(eq(12L), any(), any(), any(), any())).thenThrow(
                new ConflictoException(CodigoError.YA_INSCRIPTO, "Ya está inscripto en esta clase"));

        mockMvc.perform(post("/api/clases/12/inscripciones").header("Authorization", socio()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("YA_INSCRIPTO"));
    }

    @Test
    void inscribir_unSocioAOtro_403() throws Exception {
        when(inscripcionesDeClase.inscribir(eq(12L), any(), any(), any(), any())).thenThrow(new AccessDeniedException("Un socio solo se inscribe a sí mismo"));

        mockMvc.perform(post("/api/clases/12/inscripciones").header("Authorization", socio()).contentType(JSON).content("{\"socioId\":99}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelar_204_comoSocio() throws Exception {
        mockMvc.perform(delete("/api/clases/12/inscripciones/3").header("Authorization", socio())).andExpect(status().isNoContent());
    }

    @Test
    void cancelar_unaInscripcionAjena_403() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Un socio solo cancela su propia inscripción"))
                .when(inscripcionesDeClase).cancelar(12L, CodigoError.CLASE_NO_ENCONTRADA, 99L,
                        new com.tenisahora.shared.dto.Actor(ID_SOCIO, com.tenisahora.shared.model.Rol.SOCIO));

        mockMvc.perform(delete("/api/clases/12/inscripciones/99").header("Authorization", socio())).andExpect(status().isForbidden());
    }

    @Test
    void cancelar_comoEmpleado_204() throws Exception {
        mockMvc.perform(delete("/api/clases/12/inscripciones/99").header("Authorization", empleado())).andExpect(status().isNoContent());
    }
}
