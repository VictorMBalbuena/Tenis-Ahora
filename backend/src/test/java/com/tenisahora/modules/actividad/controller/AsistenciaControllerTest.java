package com.tenisahora.modules.actividad.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.actividad.dto.AsistenciaResponse;
import com.tenisahora.modules.actividad.service.RegistroDeAsistencias;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsistenciaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class AsistenciaControllerTest {

    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_ENTRENADOR = 5L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RegistroDeAsistencias registroDeAsistencias;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(eq(ID_ENTRENADOR), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ENTRENADOR"));
    }

    private String entrenador() {
        return "Bearer " + jwtService.generarToken(ID_ENTRENADOR, "ENTRENADOR");
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, "SOCIO");
    }

    @Test
    void registrar_conRolSocio_403() throws Exception {
        mockMvc.perform(post("/api/actividades/10/asistencias").header("Authorization", socio()).contentType(JSON)
                        .content("{\"marcas\":[{\"socioId\":3,\"presente\":true}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrar_200_marcandoPresentesYAusentes() throws Exception {
        when(registroDeAsistencias.registrar(eq(10L), any(), any(), any())).thenReturn(List.of(
                new AsistenciaResponse(10L, 3L, "Gómez, Ana", LocalDate.now(), true)));

        mockMvc.perform(post("/api/actividades/10/asistencias").header("Authorization", entrenador()).contentType(JSON)
                        .content("{\"marcas\":[{\"socioId\":3,\"presente\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presente").value(true));
    }

    @Test
    void registrar_loteVacio_400ConElCampo() throws Exception {
        mockMvc.perform(post("/api/actividades/10/asistencias").header("Authorization", entrenador()).contentType(JSON)
                        .content("{\"marcas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.marcas").exists());
    }

    @Test
    void registrar_fechaQueNoCorresponde_422ConCodigo() throws Exception {
        when(registroDeAsistencias.registrar(eq(10L), any(), any(), any()))
                .thenThrow(new ReglaDeNegocioException(CodigoError.FECHA_SIN_DICTADO, "La actividad no se dicta ese día"));

        mockMvc.perform(post("/api/actividades/10/asistencias").header("Authorization", entrenador()).contentType(JSON)
                        .content("{\"fecha\":\"2026-10-20\",\"marcas\":[{\"socioId\":3,\"presente\":true}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FECHA_SIN_DICTADO"));
    }

    @Test
    void registrar_socioNoInscripto_422() throws Exception {
        when(registroDeAsistencias.registrar(eq(10L), any(), any(), any()))
                .thenThrow(new ReglaDeNegocioException(CodigoError.SOCIO_NO_INSCRIPTO, "No está inscripto"));

        mockMvc.perform(post("/api/actividades/10/asistencias").header("Authorization", entrenador()).contentType(JSON)
                        .content("{\"marcas\":[{\"socioId\":99,\"presente\":true}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("SOCIO_NO_INSCRIPTO"));
    }

    @Test
    void registrar_actividadAjenaOInexistente_404() throws Exception {
        when(registroDeAsistencias.registrar(eq(10L), any(), any(), any()))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.ACTIVIDAD_NO_ENCONTRADA, "No encontrada"));

        mockMvc.perform(post("/api/actividades/10/asistencias").header("Authorization", entrenador()).contentType(JSON)
                        .content("{\"marcas\":[{\"socioId\":3,\"presente\":true}]}"))
                .andExpect(status().isNotFound());
    }
}
