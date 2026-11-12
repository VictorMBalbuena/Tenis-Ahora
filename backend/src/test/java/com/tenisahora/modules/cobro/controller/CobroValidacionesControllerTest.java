package com.tenisahora.modules.cobro.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.service.AnulacionDeCobros;
import com.tenisahora.modules.cobro.service.CobroDeInscripcionActividad;
import com.tenisahora.modules.cobro.service.CobroDeInscripcionTorneo;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.modules.cobro.service.QrDeCobro;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Valores límite y permisos de la API de cobros: lo que la pantalla valida, la API lo tiene que rechazar también.
@WebMvcTest({CobroController.class, CobrosController.class, CobrosDeReservaController.class, CobrosDeInscripcionController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class CobroValidacionesControllerTest {

    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_ADMIN = 5L;
    private static final String RESERVA = "/api/reservas/10/cobros";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AnulacionDeCobros anulacionDeCobros;
    @MockitoBean
    private CobroService cobroService;
    @MockitoBean
    private QrDeCobro qrDeCobro;
    @MockitoBean
    private CobroDeInscripcionActividad cobroDeActividad;
    @MockitoBean
    private CobroDeInscripcionTorneo cobroDeTorneo;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(eq(ID_ADMIN), anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        CobroResponse cobro = new CobroResponse(1L, "C-000001", 10L, null, ConceptoCobro.SEÑA, EstadoCobro.PAGADO, MedioPago.DEBITO, null,
                new BigDecimal("8000.00"), new BigDecimal("8000.00"), null, "000123", LocalDateTime.now(), null, null, null, null, null);
        when(cobroService.registrar(any(), any(), any())).thenReturn(cobro);
        when(cobroDeActividad.registrar(any(), any(), any())).thenReturn(cobro);
        when(cobroDeTorneo.registrar(any(), any(), any())).thenReturn(cobro);
        when(anulacionDeCobros.anular(any(), any(), any())).thenReturn(cobro);
        when(cobroService.buscar(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
    }

    private String empleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, Rol.EMPLEADO.name());
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, Rol.SOCIO.name());
    }

    private String admin() {
        return "Bearer " + jwtService.generarToken(ID_ADMIN, Rol.ADMINISTRADOR.name());
    }

    private MockHttpServletRequestBuilder cobrar(String url, String cuerpo) {
        return post(url).header("Authorization", empleado()).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private MockHttpServletRequestBuilder anular(String token, String cuerpo) {
        return post("/api/cobros/1/anulacion").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a", "", " ", "-1", "1.5", "12 34", "123456789012345678901", "１２３"})
    void comprobante_invalido_400EnReserva(String comprobante) throws Exception {
        mockMvc.perform(cobrar(RESERVA, "{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"" + comprobante + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.numeroOperacion").exists());
        verify(cobroService, never()).registrar(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "000123", "12345678901234567890"})
    void comprobante_valido_201(String comprobante) throws Exception {
        mockMvc.perform(cobrar(RESERVA, "{\"medioPago\":\"CREDITO\",\"numeroOperacion\":\"" + comprobante + "\"}"))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/actividades/inscripciones/1/cobros", "/api/torneos/inscripciones/1/cobros"})
    void comprobante_invalido_400TambienEnInscripciones(String url) throws Exception {
        mockMvc.perform(cobrar(url, "{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/actividades/inscripciones/1/cobros", "/api/torneos/inscripciones/1/cobros"})
    void inscripcion_valida_201ConLocation(String url) throws Exception {
        mockMvc.perform(cobrar(url, "{\"medioPago\":\"EFECTIVO\"}")).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 25, 100})
    void cuotas_fueraDeRango_400(int cuotas) throws Exception {
        mockMvc.perform(cobrar(RESERVA, "{\"medioPago\":\"CREDITO\",\"numeroOperacion\":\"1\",\"cuotas\":" + cuotas + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cuotas").exists());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12, 13, 24})
    void cuotas_enLosLimites_201(int cuotas) throws Exception {
        mockMvc.perform(cobrar(RESERVA, "{\"medioPago\":\"CREDITO\",\"numeroOperacion\":\"1\",\"cuotas\":" + cuotas + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void cuotas_noNumericas_400() throws Exception {
        mockMvc.perform(cobrar(RESERVA, "{\"medioPago\":\"CREDITO\",\"numeroOperacion\":\"1\",\"cuotas\":\"tres\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBITO", "EFECTIVO", "QR"})
    void cuotas_conOtroMedioQueCredito_400(String medio) throws Exception {
        mockMvc.perform(cobrar(RESERVA, "{\"medioPago\":\"" + medio + "\",\"numeroOperacion\":\"1\",\"cuotas\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cuotasConCredito").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"medioPago\":\"BITCOIN\"}", "{\"medioPago\":\"\"}", "{\"medioPago\":null}", "{}", "no es json", ""})
    void medioDePago_ausenteOInvalido_400(String cuerpo) throws Exception {
        mockMvc.perform(cobrar(RESERVA, cuerpo)).andExpect(status().isBadRequest());
        verify(cobroService, never()).registrar(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void idDeReserva_invalido_400(String id) throws Exception {
        mockMvc.perform(cobrar("/api/reservas/" + id + "/cobros", "{\"medioPago\":\"EFECTIVO\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void anular_sinToken_401() throws Exception {
        mockMvc.perform(post("/api/cobros/1/anulacion").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anular_empleadoOSocio_403_soloElAdministrador() throws Exception {
        mockMvc.perform(anular(empleado(), "{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(anular(socio(), "{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
        verify(anulacionDeCobros, never()).anular(any(), any(), any());
    }

    @Test
    void anular_motivoDeUnoYDe255_200() throws Exception {
        mockMvc.perform(anular(admin(), "{\"motivo\":\"x\"}")).andExpect(status().isOk());
        mockMvc.perform(anular(admin(), "{\"motivo\":\"" + "x".repeat(255) + "\"}")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"motivo\":null}", "{\"motivo\":\"\"}", "{\"motivo\":\"   \"}"})
    void anular_sinMotivo_400(String cuerpo) throws Exception {
        mockMvc.perform(anular(admin(), cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").exists());
        verify(anulacionDeCobros, never()).anular(any(), any(), any());
    }

    @Test
    void anular_motivoDe256_400() throws Exception {
        mockMvc.perform(anular(admin(), "{\"motivo\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").exists());
    }

    @Test
    void listar_sinTokenOSocio_rechazado_elListadoEsDelPersonal() throws Exception {
        mockMvc.perform(get("/api/cobros")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cobros").header("Authorization", socio())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=51", "page=abc", "desde=ayer", "hasta=2026-13-40", "medioPago=BITCOIN", "concepto=NADA"})
    void listar_filtroInvalido_400(String filtro) throws Exception {
        mockMvc.perform(get("/api/cobros?" + filtro).header("Authorization", empleado())).andExpect(status().isBadRequest());
        verify(cobroService, never()).buscar(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0", "size=1", "size=50", "desde=2026-11-07&hasta=2026-11-07", "medioPago=EFECTIVO", "concepto=SALDO"})
    void listar_filtroEnElLimite_200(String filtro) throws Exception {
        mockMvc.perform(get("/api/cobros?" + filtro).header("Authorization", admin())).andExpect(status().isOk());
    }
}
