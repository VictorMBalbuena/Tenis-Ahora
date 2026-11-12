package com.tenisahora.modules.socio.controller;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.config.CorsConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.socio.dto.SocioResponse;
import com.tenisahora.shared.dto.PersonaRequest;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.socio.service.SocioService;
import com.tenisahora.security.JwtService;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SocioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class SocioControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;

    private static final String ALTA_VALIDA = """
            {"nombre":"Ana","apellido":"Gómez","dni":"30123456","calle":"Av. Siempreviva",
             "numeroCalle":"742","codigoArea":"11","numeroTelefono":"12345678","email":"ana@correo.com","rol":"SOCIO","password":"Clave-segura-123"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SocioService socioService;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_ADMIN), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
    }

    private String bearer(long id, Rol rol) {
        Socio persona = new Socio();
        persona.setId(id);
        return "Bearer " + jwtService.generarToken(persona.getId(), rol.name());
    }

    private String empleado() {
        return bearer(ID_EMPLEADO, Rol.EMPLEADO);
    }

    private SocioResponse respuesta(Long id) {
        return new SocioResponse(id, "Ana", "Gómez", "30123456", null, null, "Av. Siempreviva",
                "742", "11", "12345678", "ana@correo.com", Rol.SOCIO, true, LocalDate.of(2026, 10, 10), "S-0012");
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/api/socios/12"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void conRolSocio_403_enTodosLosEndpoints() throws Exception {
        String socio = bearer(ID_SOCIO, Rol.SOCIO);

        mockMvc.perform(get("/api/socios").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/socios/12").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/socios").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/socios/12").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/socios/12").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/socios/12/pdf").header("Authorization", socio)).andExpect(status().isForbidden());
    }

    @Test
    void conRolSocio_y_unCuerpoInvalido_403_nuncaSeLlegaAValidar() throws Exception {
        // Si primero se validara el cuerpo, un rol sin permiso podría sondear las reglas de validación
        mockMvc.perform(post("/api/socios").header("Authorization", bearer(ID_SOCIO, Rol.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void conRolSocio_elServicioNoSeLlama() throws Exception {
        mockMvc.perform(delete("/api/socios/12").header("Authorization", bearer(ID_SOCIO, Rol.SOCIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));

        verify(socioService, never()).eliminar(any(), any());
    }

    // ---------- CU01 ----------

    @Test
    void registrar_201_conLocationYSinCache() throws Exception {
        when(socioService.registrar(any(PersonaRequest.class), any(Actor.class))).thenReturn(respuesta(12L));

        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/socios/12"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.numeroSocio").value("S-0012"));
    }

    @Test
    void registrar_leDiceAlServicioQuienLoHace() throws Exception {
        when(socioService.registrar(any(PersonaRequest.class), any(Actor.class))).thenReturn(respuesta(12L));

        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA));

        verify(socioService).registrar(any(PersonaRequest.class), eq(new Actor(ID_EMPLEADO, Rol.EMPLEADO)));
    }

    @Test
    void registrar_larespuestaNuncaTraeLaContrasenia() throws Exception {
        when(socioService.registrar(any(PersonaRequest.class), any(Actor.class))).thenReturn(respuesta(12L));

        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrar_sinCamposObligatorios_400ConElDetalleDeCadaCampo() throws Exception {
        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.dni").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.password").exists())
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.ruta").value("/api/socios"));
        verify(socioService, never()).registrar(any(), any());
    }

    @Test
    void registrar_emailYDniInvalidos_400() throws Exception {
        String cuerpo = ALTA_VALIDA.replace("ana@correo.com", "no-es-un-correo").replace("30123456", "12ab");

        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.dni").value("El DNI debe tener 7 u 8 números"));
    }

    @Test
    void registrar_contraseniaCorta_400() throws Exception {
        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA.replace("Clave-segura-123", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void registrar_conUnJsonRoto_400() throws Exception {
        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
    }

    @Test
    void registrar_dniRepetido_409ConSuCodigo() throws Exception {
        when(socioService.registrar(any(PersonaRequest.class), any(Actor.class)))
                .thenThrow(new ConflictoException(CodigoError.PERSONA_DNI_DUPLICADO, "Ya existe una persona con ese DNI"));

        mockMvc.perform(post("/api/socios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERSONA_DNI_DUPLICADO"))
                .andExpect(jsonPath("$.mensaje").value("Ya existe una persona con ese DNI"));
    }

    // ---------- CU03 ----------

    @Test
    void buscar_200_conLaPaginaYLosFiltros() throws Exception {
        PageResponse<PersonaResumenResponse> pagina = new PageResponse<>(
                List.of(new PersonaResumenResponse(12L, "Ana", "Gómez", "30123456", "ana@correo.com", Rol.SOCIO, true)),
                0, 20, 1, 1);
        when(socioService.buscar("30123456", null, 0, 20)).thenReturn(pagina);

        mockMvc.perform(get("/api/socios").param("dni", "30123456").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.contenido[0].id").value(12))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].passwordHash").doesNotExist());
    }

    @Test
    void buscar_tamanioMayorAlTope_400() throws Exception {
        mockMvc.perform(get("/api/socios").param("size", "51").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.size").exists());
        verify(socioService, never()).buscar(any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void buscar_paginaNegativa_400() throws Exception {
        mockMvc.perform(get("/api/socios").param("page", "-1").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.page").exists());
    }

    @Test
    void consultar_200() throws Exception {
        when(socioService.consultar(12L)).thenReturn(respuesta(12L));

        mockMvc.perform(get("/api/socios/12").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("30123456"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void consultar_inexistente_404() throws Exception {
        when(socioService.consultar(99L))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));

        mockMvc.perform(get("/api/socios/99").header("Authorization", empleado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PERSONA_NO_ENCONTRADA"));
    }

    @Test
    void consultar_idInvalido_400() throws Exception {
        mockMvc.perform(get("/api/socios/0").header("Authorization", empleado()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/socios/abc").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
    }

    // ---------- CU02 ----------

    @Test
    void modificar_200() throws Exception {
        when(socioService.modificar(eq(12L), any(PersonaUpdateRequest.class), any(Actor.class))).thenReturn(respuesta(12L));

        mockMvc.perform(patch("/api/socios/12").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoArea\":\"11\",\"numeroTelefono\":\"55556666\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void modificar_emailInvalido_400() throws Exception {
        mockMvc.perform(patch("/api/socios/12").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"mal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());
        verify(socioService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_noPermiteCambiarElRolNiElEstado() throws Exception {
        when(socioService.modificar(eq(12L), any(PersonaUpdateRequest.class), any(Actor.class))).thenReturn(respuesta(12L));

        // "rol" y "activo" no existen en el pedido de modificación: se ignoran, no se aplican
        mockMvc.perform(patch("/api/socios/12").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMINISTRADOR\",\"activo\":false}"))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(
                java.util.Arrays.stream(PersonaUpdateRequest.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("rol", "activo", "password");
    }

    @Test
    void modificar_inexistente_404() throws Exception {
        when(socioService.modificar(eq(99L), any(PersonaUpdateRequest.class), any(Actor.class)))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));

        mockMvc.perform(patch("/api/socios/99").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- CU04 ----------

    @Test
    void eliminar_204SinCuerpo() throws Exception {
        mockMvc.perform(delete("/api/socios/12").header("Authorization", empleado()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(socioService).eliminar(12L, new Actor(ID_EMPLEADO, Rol.EMPLEADO));
    }

    @Test
    void eliminar_conHistorial_409() throws Exception {
        doThrow(new ConflictoException(CodigoError.PERSONA_CON_HISTORIAL, "Deshabilitala")).when(socioService).eliminar(eq(12L), any(Actor.class));

        mockMvc.perform(delete("/api/socios/12").header("Authorization", empleado()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERSONA_CON_HISTORIAL"));
    }

    // ---------- CU05 ----------

    @Test
    void imprimir_devuelveUnPdfParaDescargar() throws Exception {
        byte[] pdf = "%PDF-1.4 contenido".getBytes();
        when(socioService.imprimir(12L)).thenReturn(pdf);

        mockMvc.perform(get("/api/socios/12/pdf").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"socio-12.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void imprimir_inexistente_404() throws Exception {
        when(socioService.imprimir(99L))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));

        mockMvc.perform(get("/api/socios/99/pdf").header("Authorization", empleado()))
                .andExpect(status().isNotFound());
    }

    // ---------- Sesión ----------

    @Test
    void conUnTokenDeUnaCuentaEliminada_401() throws Exception {
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/socios/12").header("Authorization", empleado()))
                .andExpect(status().isUnauthorized());
    }
}
