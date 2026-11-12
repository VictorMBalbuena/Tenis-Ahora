package com.tenisahora.modules.actividad.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.actividad.dto.InstructorDisponibleResponse;
import com.tenisahora.modules.actividad.service.AsignacionDeInstructorAActividad;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsignacionDeInstructorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class AsignacionDeInstructorControllerTest {

    private static final long ID_EMPLEADO = 2L;
    private static final long ID_ENTRENADOR = 5L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AsignacionDeInstructorAActividad asignacion;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_ENTRENADOR), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ENTRENADOR"));
    }

    private String empleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, "EMPLEADO");
    }

    private String entrenador() {
        return "Bearer " + jwtService.generarToken(ID_ENTRENADOR, "ENTRENADOR");
    }

    @Test
    void asignarInstructor_conRolEntrenador_403() throws Exception {
        mockMvc.perform(put("/api/actividades/10/instructor").header("Authorization", entrenador()).contentType(JSON)
                        .content("{\"rol\":\"ENTRENADOR\",\"instructorId\":5}"))
                .andExpect(status().isForbidden());
        verify(asignacion, never()).asignar(any(), any(), any());
    }

    @Test
    void disponibles_200() throws Exception {
        when(asignacion.disponibles(10L)).thenReturn(List.of(new InstructorDisponibleResponse(5L, "Apellido5, Nombre5", Rol.ENTRENADOR)));

        mockMvc.perform(get("/api/actividades/10/instructores-disponibles").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rol").value("ENTRENADOR"));
    }

    @Test
    void asignar_200_comoEmpleado() throws Exception {
        mockMvc.perform(put("/api/actividades/10/instructor").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"rol\":\"ENTRENADOR\",\"instructorId\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    void asignar_sinCertificacionValidada_422() throws Exception {
        org.mockito.Mockito.doThrow(new ReglaDeNegocioException(CodigoError.INSTRUCTOR_NO_VALIDADO, "Sin certificación validada"))
                .when(asignacion).asignar(eq(10L), any(), any());

        mockMvc.perform(put("/api/actividades/10/instructor").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"rol\":\"ENTRENADOR\",\"instructorId\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("INSTRUCTOR_NO_VALIDADO"));
    }

    @Test
    void asignar_sobreUnaGrupal_422_MODALIDAD_INVALIDA() throws Exception {
        org.mockito.Mockito.doThrow(new ReglaDeNegocioException(CodigoError.MODALIDAD_INVALIDA, "No es particular"))
                .when(asignacion).asignar(eq(10L), any(), any());

        mockMvc.perform(put("/api/actividades/10/instructor").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"rol\":\"ENTRENADOR\",\"instructorId\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MODALIDAD_INVALIDA"));
    }
}
