package com.tenisahora.modules.cobro.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.cobro.dto.QrResponse;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.EstadoRecibo;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.modules.cobro.service.QrDeCobro;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CobroController.class, CobrosDeReservaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class CobrosControllerTest {

    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;
    private static final Actor SOCIO = new Actor(ID_SOCIO, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(ID_EMPLEADO, Rol.EMPLEADO);
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private com.tenisahora.modules.cobro.service.AnulacionDeCobros anulacionDeCobros;
    @MockitoBean
    private CobroService cobroService;
    @MockitoBean
    private QrDeCobro qrDeCobro;
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

    private String bearer(long id, Rol rol) {
        return "Bearer " + jwtService.generarToken(id, rol.name());
    }

    private String socio() {
        return bearer(ID_SOCIO, Rol.SOCIO);
    }

    private String empleado() {
        return bearer(ID_EMPLEADO, Rol.EMPLEADO);
    }

    private static CobroResponse cobro(Long id, EstadoCobro estado) {
        return new CobroResponse(id, "C-000" + id, 10L, null, ConceptoCobro.SEÑA, estado, MedioPago.DEBITO, null,
                new BigDecimal("8000.00"), new BigDecimal("8000.00"), null, "000123", LocalDateTime.now(), null, null, null, null, null);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(get("/api/cobros/1")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mockMvc.perform(get("/api/cobros/1/recibo")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cobros/1/qr")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reservas/10/cobros").contentType(JSON).content("{\"medioPago\":\"DEBITO\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conRolProfesor_403_enTodosLosEndpoints() throws Exception {
        String profesor = bearer(ID_PROFESOR, Rol.PROFESOR);

        mockMvc.perform(get("/api/cobros/1").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cobros/1/qr").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/10/cobros").header("Authorization", profesor).contentType(JSON).content("{\"medioPago\":\"DEBITO\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- CU51: registrar ----------

    @Test
    void registrar_201_conLocationYSinCache() throws Exception {
        when(cobroService.registrar(eq(10L), any(), eq(EMPLEADO))).thenReturn(cobro(7L, EstadoCobro.PAGADO));

        mockMvc.perform(post("/api/reservas/10/cobros").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"000123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/cobros/7")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void registrar_socioConDebito_403_noPuedeAutoaprobarseElPago() throws Exception {
        when(cobroService.registrar(eq(10L), any(), eq(SOCIO)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Solo el personal del club cobra con débito o crédito"));

        mockMvc.perform(post("/api/reservas/10/cobros").header("Authorization", socio()).contentType(JSON)
                        .content("{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"000123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));
    }

    @Test
    void registrar_sinMedioDePago_400() throws Exception {
        mockMvc.perform(post("/api/reservas/10/cobros").header("Authorization", empleado()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.medioPago").exists());
        verify(cobroService, never()).registrar(any(), any(), any());
    }

    @Test
    void registrar_erroresDelServicio_404_409_422() throws Exception {
        when(cobroService.registrar(eq(11L), any(), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        when(cobroService.registrar(eq(12L), any(), any())).thenThrow(
                new ConflictoException(CodigoError.COBRO_YA_REGISTRADO, "La reserva ya tiene un cobro registrado"));
        when(cobroService.registrar(eq(13L), any(), any())).thenThrow(
                new ReglaDeNegocioException(CodigoError.RESERVA_NO_COBRABLE, "La reserva no admite el cobro de la seña"));

        mockMvc.perform(post("/api/reservas/11/cobros").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"1\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/reservas/12/cobros").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"1\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("COBRO_YA_REGISTRADO"));
        mockMvc.perform(post("/api/reservas/13/cobros").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"medioPago\":\"DEBITO\",\"numeroOperacion\":\"1\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RESERVA_NO_COBRABLE"));
    }

    // ---------- Consultas ----------

    @Test
    void consultar_200_paraSocioYEmpleado() throws Exception {
        when(cobroService.consultar(7L, SOCIO)).thenReturn(cobro(7L, EstadoCobro.PENDIENTE));
        when(cobroService.consultar(7L, EMPLEADO)).thenReturn(cobro(7L, EstadoCobro.PENDIENTE));

        mockMvc.perform(get("/api/cobros/7").header("Authorization", socio())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")));
        mockMvc.perform(get("/api/cobros/7").header("Authorization", empleado())).andExpect(status().isOk());
    }

    @Test
    void consultar_unCobroAjeno_404() throws Exception {
        when(cobroService.consultar(9L, SOCIO)).thenThrow(
                new RecursoNoEncontradoException(CodigoError.COBRO_NO_ENCONTRADO, "Cobro no encontrado"));

        mockMvc.perform(get("/api/cobros/9").header("Authorization", socio()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("COBRO_NO_ENCONTRADO"));
    }

    @Test
    void recibo_200_y_404SiNoSeEmitioTodavia() throws Exception {
        ReciboResponse recibo = new ReciboResponse(1L, "R-000007", EstadoRecibo.EMITIDO, new BigDecimal("8000.00"), "SEÑA",
                MedioPago.DEBITO, "000123", null, LocalDateTime.now(), null);
        when(cobroService.recibo(7L, EMPLEADO)).thenReturn(recibo);
        when(cobroService.recibo(8L, EMPLEADO)).thenThrow(
                new RecursoNoEncontradoException(CodigoError.RECIBO_NO_EMITIDO, "El cobro todavía no tiene recibo"));

        mockMvc.perform(get("/api/cobros/7/recibo").header("Authorization", empleado())).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroRecibo").value("R-000007"));
        mockMvc.perform(get("/api/cobros/8/recibo").header("Authorization", empleado()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RECIBO_NO_EMITIDO"));
    }

    // ---------- CU77/CU88: QR ----------

    @Test
    void qr_200_conElQrData() throws Exception {
        when(qrDeCobro.generar(7L, EMPLEADO)).thenReturn(new QrResponse("data-del-qr", null, EstadoCobro.PENDIENTE));

        mockMvc.perform(post("/api/cobros/7/qr").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrData").value("data-del-qr"));
    }

    @Test
    void qr_cobroQueNoAdmiteQr_422() throws Exception {
        when(qrDeCobro.generar(7L, EMPLEADO)).thenThrow(
                new ReglaDeNegocioException(CodigoError.COBRO_NO_ADMITE_QR, "El cobro no admite generar un QR nuevo"));

        mockMvc.perform(post("/api/cobros/7/qr").header("Authorization", empleado()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("COBRO_NO_ADMITE_QR"));
    }

    @Test
    void qr_pasarelaCaida_503() throws Exception {
        when(qrDeCobro.generar(7L, EMPLEADO)).thenThrow(
                new com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException("no responde"));

        mockMvc.perform(post("/api/cobros/7/qr").header("Authorization", empleado()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("PASARELA_NO_DISPONIBLE"));
    }

    @Test
    void consultar_conIdNoPositivo_400() throws Exception {
        mockMvc.perform(get("/api/cobros/0").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }
}
