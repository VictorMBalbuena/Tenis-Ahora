package com.tenisahora.modules.actividad.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.ClaseRequest;
import com.tenisahora.modules.actividad.dto.ClaseResponse;
import com.tenisahora.modules.actividad.dto.MiClaseResponse;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.modules.actividad.service.ClaseService;
import com.tenisahora.modules.actividad.service.ClasesDelProfesor;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ClaseController.class, MisClasesController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class ClasesControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private static final String ALTA_VALIDA = """
            {"nombre":"Tenis para principiantes","modalidad":"GRUPAL","nivel":"INICIAL","canchaId":1,"profesorId":4,
             "fechaInicio":"2026-11-02","fechaFin":"2027-03-02","dias":["MONDAY"],"horaInicio":"18:00","horaFin":"19:00"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ClaseService claseService;
    @MockitoBean
    private ClasesDelProfesor clasesDelProfesor;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_ADMIN), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(eq(ID_PROFESOR), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("PROFESOR"));
    }

    private String empleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, "EMPLEADO");
    }

    private String profesor() {
        return "Bearer " + jwtService.generarToken(ID_PROFESOR, "PROFESOR");
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, "SOCIO");
    }

    private static ClaseResponse clase(Long id) {
        return new ClaseResponse(id, String.format("A-%06d", id), "Tenis para principiantes", Modalidad.GRUPAL, NivelClase.INICIAL,
                EstadoActividad.PROGRAMADA, 1L, "Cancha 1", 4L, "Apellido4, Nombre4", LocalDate.of(2026, 11, 2),
                LocalDate.of(2027, 3, 2), Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(19, 0), 20, null, 0, 20, null, null);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(get("/api/clases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/clases").contentType(JSON).content(ALTA_VALIDA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mockMvc.perform(get("/api/clases/mis-clases")).andExpect(status().isUnauthorized());
    }

    @Test
    void misClases_conRolEmpleado_403() throws Exception {
        mockMvc.perform(get("/api/clases/mis-clases").header("Authorization", empleado())).andExpect(status().isForbidden());
        verify(clasesDelProfesor, never()).misClases(any(), anyInt(), anyInt());
    }

    @Test
    void crearClase_conRolProfesor_403() throws Exception {
        mockMvc.perform(post("/api/clases").header("Authorization", profesor()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isForbidden());
        verify(claseService, never()).programar(any(), any());
    }

    @Test
    void crearClase_conRolSocio_403() throws Exception {
        mockMvc.perform(post("/api/clases").header("Authorization", socio()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isForbidden());
    }

    // ---------- CU32 ----------

    @Test
    void programar_201_conLocationYSinCache() throws Exception {
        when(claseService.programar(any(ClaseRequest.class), any())).thenReturn(clase(12L));

        mockMvc.perform(post("/api/clases").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/clases/12")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.codigo").value("A-000012"));
    }

    @Test
    void programar_sinDias_400ConElCampo() throws Exception {
        String sinDias = """
                {"nombre":"X","modalidad":"GRUPAL","nivel":"INICIAL","canchaId":1,"profesorId":4,
                 "fechaInicio":"2026-11-02","fechaFin":"2027-03-02","dias":[],"horaInicio":"18:00","horaFin":"19:00"}
                """;

        mockMvc.perform(post("/api/clases").header("Authorization", empleado()).contentType(JSON).content(sinDias))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.dias").exists());
        verify(claseService, never()).programar(any(), any());
    }

    @Test
    void programar_erroresDelServicio_404Profesor_409Ocupada_422NoValidado() throws Exception {
        when(claseService.programar(any(), any()))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Profesor no encontrado"))
                .thenThrow(new ConflictoException(CodigoError.CANCHA_OCUPADA, "Ese horario ya lo ocupa otra clase"))
                .thenThrow(new ReglaDeNegocioException(CodigoError.PROFESOR_NO_VALIDADO, "El profesor no tiene la certificación validada"));

        mockMvc.perform(post("/api/clases").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/clases").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CANCHA_OCUPADA"));
        mockMvc.perform(post("/api/clases").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("PROFESOR_NO_VALIDADO"));
    }

    // ---------- CU34 ----------

    @Test
    void consultar_200_comoEmpleado_y_404SiNoExiste() throws Exception {
        when(claseService.consultar(12L)).thenReturn(clase(12L));
        when(claseService.consultar(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.CLASE_NO_ENCONTRADA, "Clase no encontrada"));

        mockMvc.perform(get("/api/clases/12").header("Authorization", empleado())).andExpect(status().isOk());
        mockMvc.perform(get("/api/clases/99").header("Authorization", empleado())).andExpect(status().isNotFound());
    }

    @Test
    void consultar_200_comoProfesor() throws Exception {
        when(claseService.consultar(12L)).thenReturn(clase(12L));

        mockMvc.perform(get("/api/clases/12").header("Authorization", profesor())).andExpect(status().isOk());
    }

    @Test
    void buscar_200_conFiltros() throws Exception {
        when(claseService.buscar(1L, 4L, null, DayOfWeek.MONDAY, null, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(clase(12L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/clases?canchaId=1&profesorId=4&dia=MONDAY").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    // ---------- CU35 ----------

    @Test
    void eliminar_204_y_409SiTieneInscriptos() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictoException(CodigoError.CLASE_CON_INSCRIPTOS, "No se puede eliminar"))
                .when(claseService).eliminar(99L);

        mockMvc.perform(delete("/api/clases/12").header("Authorization", empleado())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/clases/99").header("Authorization", empleado()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CLASE_CON_INSCRIPTOS"));
    }

    // ---------- CU36 ----------

    @Test
    void imprimir_200_conContentTypeYDisposition() throws Exception {
        when(claseService.imprimir(12L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/clases/12/pdf").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("clase-12.pdf")));
    }

    @Test
    void imprimir_200_comoProfesor() throws Exception {
        when(claseService.imprimir(12L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/clases/12/pdf").header("Authorization", profesor()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    // ---------- CU81 ----------

    @Test
    void misClases_200_comoProfesor() throws Exception {
        MiClaseResponse miClase = new MiClaseResponse(12L, "A-000012", "Tenis para principiantes", NivelClase.INICIAL,
                EstadoActividad.PROGRAMADA, "Cancha 1", LocalDate.of(2026, 11, 2), LocalDate.of(2027, 3, 2),
                Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(19, 0), 20, 5);
        when(clasesDelProfesor.misClases(any(), eq(0), eq(20))).thenReturn(new PageResponse<>(List.of(miClase), 0, 20, 1, 1));

        mockMvc.perform(get("/api/clases/mis-clases").header("Authorization", profesor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].cupoOcupado").value(5));
    }

    @Test
    void misClases_sinCertificacionValidada_422() throws Exception {
        when(clasesDelProfesor.misClases(any(), anyInt(), anyInt()))
                .thenThrow(new ReglaDeNegocioException(CodigoError.PROFESOR_NO_VALIDADO, "No tenés la certificación validada"));

        mockMvc.perform(get("/api/clases/mis-clases").header("Authorization", profesor()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("PROFESOR_NO_VALIDADO"));
    }

    @Test
    void inscriptos_deUnaClaseAjena_404() throws Exception {
        when(clasesDelProfesor.inscriptos(eq(99L), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.CLASE_NO_ENCONTRADA, "Clase no encontrada"));

        mockMvc.perform(get("/api/clases/99/inscriptos").header("Authorization", profesor())).andExpect(status().isNotFound());
    }

    @Test
    void inscriptos_deUnaClasePropia_200() throws Exception {
        when(clasesDelProfesor.inscriptos(eq(12L), any())).thenReturn(List.of(new AlumnoDeClaseResponse(3L, "Apellido3, Nombre3", "30000003", LocalDate.now(), null, null)));

        mockMvc.perform(get("/api/clases/12/inscriptos").header("Authorization", profesor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].socioId").value(3));
    }
}
