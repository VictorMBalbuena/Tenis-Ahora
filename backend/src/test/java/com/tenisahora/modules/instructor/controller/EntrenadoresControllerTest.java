package com.tenisahora.modules.instructor.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.instructor.dto.CertificacionResponse;
import com.tenisahora.modules.instructor.dto.EntrenadorRequest;
import com.tenisahora.modules.instructor.dto.EntrenadorResponse;
import com.tenisahora.modules.instructor.dto.EntrenadorUpdateRequest;
import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.service.EntrenadorService;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntrenadorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class EntrenadoresControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_ENTRENADOR = 4L;

    private static final String ALTA_VALIDA = """
            {"nombre":"Lucas","apellido":"Pérez","dni":"31123456","calle":"Av. Siempreviva",
             "numeroCalle":"742","codigoArea":"11","numeroTelefono":"12345678","email":"lucas@correo.com",
             "password":"Clave-segura-123","certificacionDeportiva":"Entrenador Nacional de Tenis",
             "certificacion":{"numeroCertificacion":"CERT-001","entidadEmisora":"FAT",
             "fechaEmision":"2024-01-01","fechaVencimiento":"2028-01-01"},"especialidad":"Tenis adultos"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EntrenadorService entrenadorService;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_ADMIN), anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(eq(ID_ENTRENADOR), anyString())).thenReturn(Optional.of("ENTRENADOR"));
    }

    private String bearer(long id, Rol rol) {
        Entrenador persona = new Entrenador();
        persona.setId(id);
        return "Bearer " + jwtService.generarToken(persona.getId(), rol.name());
    }

    private String empleado() {
        return bearer(ID_EMPLEADO, Rol.EMPLEADO);
    }

    private String admin() {
        return bearer(ID_ADMIN, Rol.ADMINISTRADOR);
    }

    private EntrenadorResponse respuesta(Long id, EstadoValidacion estado) {
        CertificacionResponse certificacion = new CertificacionResponse("CERT-001", "FAT",
                LocalDate.of(2024, 1, 1), LocalDate.of(2028, 1, 1), null, true);
        return new EntrenadorResponse(id, "Lucas", "Pérez", "31123456", null, null, "Av. Siempreviva",
                "742", "11", "12345678", "lucas@correo.com", Rol.ENTRENADOR, true, LocalDate.of(2026, 10, 10),
                "Entrenador Nacional de Tenis", certificacion, estado, null, null, "Tenis adultos", null);
    }

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(get("/api/entrenadores/12")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/entrenadores/12/validacion")).andExpect(status().isUnauthorized());
    }

    @Test
    void conRolSocio_403_enTodosLosEndpoints() throws Exception {
        String socio = bearer(ID_SOCIO, Rol.SOCIO);

        mockMvc.perform(get("/api/entrenadores").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entrenadores/12").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/entrenadores").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/entrenadores/12").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/entrenadores/12").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entrenadores/12/pdf").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/entrenadores/12/habilitacion").header("Authorization", socio)).andExpect(status().isForbidden());
    }

    @Test
    void validacion_conRolEmpleado_403() throws Exception {
        mockMvc.perform(post("/api/entrenadores/12/validacion").header("Authorization", empleado()))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrar_201_conLocationYSinCache() throws Exception {
        when(entrenadorService.registrar(any(EntrenadorRequest.class), any(Actor.class)))
                .thenReturn(respuesta(12L, EstadoValidacion.PENDIENTE));

        mockMvc.perform(post("/api/entrenadores").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/entrenadores/12"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.estadoValidacion").value("PENDIENTE"));
    }

    @Test
    void registrar_sinCertificacionDeportiva_400ConElCampo() throws Exception {
        String sinCertificacion = ALTA_VALIDA.replace(
                "\"certificacionDeportiva\":\"Entrenador Nacional de Tenis\",", "\"certificacionDeportiva\":\"\",");

        mockMvc.perform(post("/api/entrenadores").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(sinCertificacion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.certificacionDeportiva").exists());
    }

    @Test
    void registrar_dniRepetido_409() throws Exception {
        when(entrenadorService.registrar(any(EntrenadorRequest.class), any(Actor.class)))
                .thenThrow(new ConflictoException(CodigoError.PERSONA_DUPLICADA, "Ya existe una persona con ese DNI o correo electrónico"));

        mockMvc.perform(post("/api/entrenadores").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERSONA_DUPLICADA"));
    }

    @Test
    void registrar_certificacionVencida_422() throws Exception {
        when(entrenadorService.registrar(any(EntrenadorRequest.class), any(Actor.class)))
                .thenThrow(new ReglaDeNegocioException(CodigoError.CERTIFICACION_VENCIDA, "La certificación está vencida"));

        mockMvc.perform(post("/api/entrenadores").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CERTIFICACION_VENCIDA"));
    }

    @Test
    void consultar_200() throws Exception {
        when(entrenadorService.consultar(12L)).thenReturn(respuesta(12L, EstadoValidacion.VALIDADO));

        mockMvc.perform(get("/api/entrenadores/12").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoValidacion").value("VALIDADO"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void consultar_inexistente_404() throws Exception {
        when(entrenadorService.consultar(99L))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));

        mockMvc.perform(get("/api/entrenadores/99").header("Authorization", empleado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PERSONA_NO_ENCONTRADA"));
    }

    @Test
    void modificar_200() throws Exception {
        when(entrenadorService.modificar(eq(12L), any(EntrenadorUpdateRequest.class), any(Actor.class)))
                .thenReturn(respuesta(12L, EstadoValidacion.PENDIENTE));

        mockMvc.perform(patch("/api/entrenadores/12").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"especialidad\":\"Tenis avanzado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void eliminar_204() throws Exception {
        mockMvc.perform(delete("/api/entrenadores/12").header("Authorization", empleado()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void eliminar_conHistorial_409() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictoException(CodigoError.PERSONA_CON_HISTORIAL, "Tiene entrenamientos asignados"))
                .when(entrenadorService).eliminar(eq(12L), any(Actor.class));

        mockMvc.perform(delete("/api/entrenadores/12").header("Authorization", empleado()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERSONA_CON_HISTORIAL"));
    }

    @Test
    void imprimir_devuelveUnPdfParaDescargar() throws Exception {
        byte[] pdf = "%PDF-1.4 contenido".getBytes();
        when(entrenadorService.imprimir(12L)).thenReturn(pdf);

        mockMvc.perform(get("/api/entrenadores/12/pdf").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"entrenador-12.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void certificacion_200() throws Exception {
        when(entrenadorService.reemplazarCertificacion(eq(12L), any(), any(Actor.class)))
                .thenReturn(respuesta(12L, EstadoValidacion.PENDIENTE));
        String body = """
                {"numeroCertificacion":"CERT-002","entidadEmisora":"FAT","fechaEmision":"2024-01-01","fechaVencimiento":"2028-01-01"}
                """;

        mockMvc.perform(put("/api/entrenadores/12/certificacion").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoValidacion").value("PENDIENTE"));
    }

    @Test
    void habilitacion_pendiente_noAsignable() throws Exception {
        when(entrenadorService.habilitacion(12L)).thenReturn(HabilitacionResponse.deNoAsignable("La certificación no está validada"));

        mockMvc.perform(get("/api/entrenadores/12/habilitacion").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignable").value(false))
                .andExpect(jsonPath("$.motivo").exists());
    }

    @Test
    void validacion_aprobarConRolAdministrador_200() throws Exception {
        when(entrenadorService.decidirCertificacion(eq(12L), any(), any(Actor.class)))
                .thenReturn(respuesta(12L, EstadoValidacion.VALIDADO));

        mockMvc.perform(post("/api/entrenadores/12/validacion").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aprobada\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoValidacion").value("VALIDADO"));
    }

    @Test
    void validacion_rechazarSinMotivo_422() throws Exception {
        when(entrenadorService.decidirCertificacion(eq(12L), any(), any(Actor.class)))
                .thenThrow(new ReglaDeNegocioException(CodigoError.MOTIVO_DE_RECHAZO_REQUERIDO, "Hay que indicar el motivo del rechazo"));

        mockMvc.perform(post("/api/entrenadores/12/validacion").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aprobada\":false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_DE_RECHAZO_REQUERIDO"));
    }

    @Test
    void validacion_certificacionVencida_422() throws Exception {
        when(entrenadorService.decidirCertificacion(eq(12L), any(), any(Actor.class)))
                .thenThrow(new ReglaDeNegocioException(CodigoError.CERTIFICACION_VENCIDA, "La certificación está vencida"));

        mockMvc.perform(post("/api/entrenadores/12/validacion").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aprobada\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CERTIFICACION_VENCIDA"));
    }
}
