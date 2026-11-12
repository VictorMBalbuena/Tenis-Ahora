package com.tenisahora.modules.actividad.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.EntrenamientoRequest;
import com.tenisahora.modules.actividad.dto.EntrenamientoResponse;
import com.tenisahora.modules.actividad.dto.MiEntrenamientoResponse;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.service.EntrenamientoService;
import com.tenisahora.modules.actividad.service.EntrenamientosDelEntrenador;
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

/** Espejo de ClasesControllerTest. */
@WebMvcTest({EntrenamientoController.class, MisEntrenamientosController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class EntrenamientosControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_ENTRENADOR = 5L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private static final String ALTA_VALIDA = """
            {"nombre":"Entrenamiento funcional","modalidad":"GRUPAL","objetivo":"Mejorar el saque","canchaId":1,"entrenadorId":5,
             "fechaInicio":"2026-11-02","fechaFin":"2027-03-02","dias":["MONDAY"],"horaInicio":"18:00","horaFin":"19:00"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EntrenamientoService entrenamientoService;
    @MockitoBean
    private EntrenamientosDelEntrenador entrenamientosDelEntrenador;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_ADMIN), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(eq(ID_ENTRENADOR), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ENTRENADOR"));
    }

    private String empleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, "EMPLEADO");
    }

    private String entrenador() {
        return "Bearer " + jwtService.generarToken(ID_ENTRENADOR, "ENTRENADOR");
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, "SOCIO");
    }

    private static EntrenamientoResponse entrenamiento(Long id) {
        return new EntrenamientoResponse(id, String.format("A-%06d", id), "Entrenamiento funcional", Modalidad.GRUPAL,
                "Mejorar el saque", EstadoActividad.PROGRAMADA, 1L, "Cancha 1", 5L, "Apellido5, Nombre5", LocalDate.of(2026, 11, 2),
                LocalDate.of(2027, 3, 2), Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(19, 0), 20, null, 0, 20, null, null);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(get("/api/entrenamientos/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/entrenamientos").contentType(JSON).content(ALTA_VALIDA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mockMvc.perform(get("/api/entrenamientos/mis-entrenamientos")).andExpect(status().isUnauthorized());
    }

    @Test
    void misEntrenamientos_conRolProfesor_403() throws Exception {
        mockMvc.perform(get("/api/entrenamientos/mis-entrenamientos").header("Authorization", empleado())).andExpect(status().isForbidden());
        verify(entrenamientosDelEntrenador, never()).misEntrenamientos(any(), anyInt(), anyInt());
    }

    @Test
    void crearEntrenamiento_conRolSocio_403() throws Exception {
        mockMvc.perform(post("/api/entrenamientos").header("Authorization", socio()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isForbidden());
        verify(entrenamientoService, never()).programar(any(), any());
    }

    // ---------- CU37 ----------

    @Test
    void programar_201_conLocationYSinCache() throws Exception {
        when(entrenamientoService.programar(any(EntrenamientoRequest.class), any())).thenReturn(entrenamiento(12L));

        mockMvc.perform(post("/api/entrenamientos").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/entrenamientos/12")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.codigo").value("A-000012"));
    }

    @Test
    void programar_erroresDelServicio_404Entrenador_409Ocupada_422NoValidado() throws Exception {
        when(entrenamientoService.programar(any(), any()))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Entrenador no encontrado"))
                .thenThrow(new ConflictoException(CodigoError.CANCHA_OCUPADA, "Ese horario ya lo ocupa otra clase"))
                .thenThrow(new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "El entrenador no tiene la certificación validada"));

        mockMvc.perform(post("/api/entrenamientos").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/entrenamientos").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CANCHA_OCUPADA"));
        mockMvc.perform(post("/api/entrenamientos").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("INSTRUCTOR_NO_VALIDADO"));
    }

    // ---------- CU39 ----------

    @Test
    void consultar_200_comoEmpleado_y_404SiNoExiste() throws Exception {
        when(entrenamientoService.consultar(12L)).thenReturn(entrenamiento(12L));
        when(entrenamientoService.consultar(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, "No encontrado"));

        mockMvc.perform(get("/api/entrenamientos/12").header("Authorization", empleado())).andExpect(status().isOk());
        mockMvc.perform(get("/api/entrenamientos/99").header("Authorization", empleado())).andExpect(status().isNotFound());
    }

    @Test
    void consultar_200_comoEntrenador() throws Exception {
        when(entrenamientoService.consultar(12L)).thenReturn(entrenamiento(12L));

        mockMvc.perform(get("/api/entrenamientos/12").header("Authorization", entrenador())).andExpect(status().isOk());
    }

    @Test
    void buscar_200_conFiltros() throws Exception {
        when(entrenamientoService.buscar(1L, 5L, null, DayOfWeek.MONDAY, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(entrenamiento(12L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/entrenamientos?canchaId=1&entrenadorId=5&dia=MONDAY").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    // ---------- CU40 ----------

    @Test
    void eliminar_204_y_409SiTieneInscriptos() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictoException(CodigoError.ACTIVIDAD_CON_INSCRIPTOS, "No se puede eliminar"))
                .when(entrenamientoService).eliminar(99L);

        mockMvc.perform(delete("/api/entrenamientos/12").header("Authorization", empleado())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/entrenamientos/99").header("Authorization", empleado()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ACTIVIDAD_CON_INSCRIPTOS"));
    }

    // ---------- CU41 ----------

    @Test
    void imprimir_200_conContentTypeYDisposition() throws Exception {
        when(entrenamientoService.imprimir(12L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/entrenamientos/12/pdf").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("entrenamiento-12.pdf")));
    }

    @Test
    void imprimir_200_comoEntrenador() throws Exception {
        when(entrenamientoService.imprimir(12L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/entrenamientos/12/pdf").header("Authorization", entrenador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    // ---------- CU83 ----------

    @Test
    void misEntrenamientos_200_comoEntrenador() throws Exception {
        MiEntrenamientoResponse mio = new MiEntrenamientoResponse(12L, "A-000012", "Entrenamiento funcional", "Mejorar el saque",
                EstadoActividad.PROGRAMADA, "Cancha 1", LocalDate.of(2026, 11, 2), LocalDate.of(2027, 3, 2),
                Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(19, 0), 20, 5);
        when(entrenamientosDelEntrenador.misEntrenamientos(any(), eq(0), eq(20))).thenReturn(new PageResponse<>(List.of(mio), 0, 20, 1, 1));

        mockMvc.perform(get("/api/entrenamientos/mis-entrenamientos").header("Authorization", entrenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].cupoOcupado").value(5));
    }

    @Test
    void misEntrenamientos_sinCertificacionValidada_422() throws Exception {
        when(entrenamientosDelEntrenador.misEntrenamientos(any(), anyInt(), anyInt()))
                .thenThrow(new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "No tenés la certificación validada"));

        mockMvc.perform(get("/api/entrenamientos/mis-entrenamientos").header("Authorization", entrenador()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("INSTRUCTOR_NO_VALIDADO"));
    }

    @Test
    void inscriptos_deUnEntrenamientoAjeno_404() throws Exception {
        when(entrenamientosDelEntrenador.inscriptos(eq(99L), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.ENTRENAMIENTO_NO_ENCONTRADO, "No encontrado"));

        mockMvc.perform(get("/api/entrenamientos/99/inscriptos").header("Authorization", entrenador())).andExpect(status().isNotFound());
    }

    @Test
    void inscriptos_deUnEntrenamientoPropio_200() throws Exception {
        when(entrenamientosDelEntrenador.inscriptos(eq(12L), any()))
                .thenReturn(List.of(new AlumnoDeClaseResponse(3L, "Apellido3, Nombre3", "30000003", LocalDate.now(), null, null)));

        mockMvc.perform(get("/api/entrenamientos/12/inscriptos").header("Authorization", entrenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].socioId").value(3));
    }
}
