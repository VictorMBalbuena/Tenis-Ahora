package com.tenisahora.modules.reserva.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.CancelacionResponse;
import com.tenisahora.modules.reserva.dto.DisponibilidadReservaResponse;
import com.tenisahora.modules.reserva.dto.ReservaRequest;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.dto.ReservaUpdateRequest;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.service.CancelacionService;
import com.tenisahora.modules.reserva.service.DisponibilidadParaReservar;
import com.tenisahora.modules.reserva.service.ReservaService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReservaController.class, CancelacionController.class, DisponibilidadReservaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class ReservasControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;
    private static final Actor SOCIO = new Actor(ID_SOCIO, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(ID_EMPLEADO, Rol.EMPLEADO);
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private static final String ALTA_VALIDA = """
            {"canchaId":1,"fecha":"2026-10-22","horaInicio":"18:00","duracionHoras":2,
             "acompaniantes":[{"nombre":"Luciana","apellido":"Fernández"}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReservaService reservaService;
    @MockitoBean
    private CancelacionService cancelacionService;
    @MockitoBean
    private DisponibilidadParaReservar disponibilidad;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_ADMIN), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_EMPLEADO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_SOCIO), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("SOCIO"));
        when(verificadorDeSesion.rolVigente(org.mockito.ArgumentMatchers.eq(ID_PROFESOR), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of("PROFESOR"));
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

    private static ReservaResponse reserva(Long id) {
        return new ReservaResponse(id, "R-000" + id, 1L, "Cancha 1", LocalDate.of(2026, 10, 22), LocalTime.of(18, 0), LocalTime.of(20, 0),
                EstadoReserva.PENDIENTE_PAGO, 2, List.of(), new BigDecimal("16000.00"), null, null, new BigDecimal("8000.00"), null, null, null, null,
                List.of(), null, null, null, null, null);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(get("/api/reservas/mis-reservas")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mockMvc.perform(post("/api/reservas").contentType(JSON).content(ALTA_VALIDA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservas/disponibilidad?fecha=2026-10-22")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reservas/1/cancelacion")).andExpect(status().isUnauthorized());
    }

    @Test
    void conRolProfesor_403_enTodosLosEndpoints() throws Exception {
        String profesor = bearer(ID_PROFESOR, Rol.PROFESOR);

        mockMvc.perform(post("/api/reservas").header("Authorization", profesor).contentType(JSON).content(ALTA_VALIDA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas/mis-reservas").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas/1").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/reservas/1").header("Authorization", profesor).contentType(JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas/1/cancelacion").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/1/cancelacion").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas/disponibilidad?fecha=2026-10-22").header("Authorization", profesor)).andExpect(status().isForbidden());
    }

    @Test
    void unSocioNoBuscaNiModificaReservas_403() throws Exception {
        mockMvc.perform(get("/api/reservas").header("Authorization", socio())).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/reservas/1").header("Authorization", socio()).contentType(JSON).content("{\"duracionHoras\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));
        verify(reservaService, never()).modificar(any(), any(), any());
        verify(reservaService, never()).buscar(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void elPersonalNoUsaMisReservas_403() throws Exception {
        mockMvc.perform(get("/api/reservas/mis-reservas").header("Authorization", empleado())).andExpect(status().isForbidden());
    }

    @Test
    void conRolSocio_y_unCuerpoInvalido_403_nuncaSeLlegaAValidar() throws Exception {
        mockMvc.perform(patch("/api/reservas/1").header("Authorization", socio()).contentType(JSON).content("{\"duracionHoras\":99}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    // ---------- CU16 ----------

    @Test
    void registrar_comoSocio_201_conLocationYSinCache() throws Exception {
        when(reservaService.registrar(any(ReservaRequest.class), eq(SOCIO))).thenReturn(reserva(10L));

        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/reservas/10")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.montoSenia").value(8000.00));
    }

    @Test
    void registrar_comoEmpleado_201() throws Exception {
        when(reservaService.registrar(any(ReservaRequest.class), eq(EMPLEADO))).thenReturn(reserva(10L));

        mockMvc.perform(post("/api/reservas").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"canchaId\":1,\"fecha\":\"2026-10-22\",\"horaInicio\":\"18:00\",\"duracionHoras\":1,\"titularId\":3,"
                                + "\"acompaniantes\":[{\"personaId\":5}]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void registrar_sinDatos_400ConElDetalleDeCadaCampo() throws Exception {
        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.canchaId").exists())
                .andExpect(jsonPath("$.campos.fecha").exists())
                .andExpect(jsonPath("$.campos.horaInicio").exists())
                .andExpect(jsonPath("$.campos.duracionHoras").exists())
                .andExpect(jsonPath("$.campos.acompaniantes").exists());
        verify(reservaService, never()).registrar(any(), any());
    }

    @Test
    void registrar_conCuatroAcompaniantesOSinNinguno_400() throws Exception {
        String cuatro = "{\"canchaId\":1,\"fecha\":\"2026-10-22\",\"horaInicio\":\"18:00\",\"duracionHoras\":1,\"acompaniantes\":"
                + "[{\"nombre\":\"A\",\"apellido\":\"B\"},{\"nombre\":\"A\",\"apellido\":\"B\"},{\"nombre\":\"A\",\"apellido\":\"B\"},{\"nombre\":\"A\",\"apellido\":\"B\"}]}";
        String ninguno = "{\"canchaId\":1,\"fecha\":\"2026-10-22\",\"horaInicio\":\"18:00\",\"duracionHoras\":1,\"acompaniantes\":[]}";

        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content(cuatro)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content(ninguno)).andExpect(status().isBadRequest());
        verify(reservaService, never()).registrar(any(), any());
    }

    @Test
    void registrar_conFechaOHoraMalEscritas_400() throws Exception {
        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON)
                        .content("{\"canchaId\":1,\"fecha\":\"22/10/2026\",\"horaInicio\":\"18:00\",\"duracionHoras\":1,\"acompaniantes\":[{\"nombre\":\"A\",\"apellido\":\"B\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
    }

    @Test
    void registrar_ignoraEstadoMontosYOtrosCamposQueNoSonDelAlta() throws Exception {
        when(reservaService.registrar(any(ReservaRequest.class), eq(SOCIO))).thenReturn(reserva(10L));

        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON)
                        .content("{\"estado\":\"CONFIRMADA\",\"montoTotal\":1,\"id\":99,\"canchaId\":1,\"fecha\":\"2026-10-22\",\"horaInicio\":\"18:00\","
                                + "\"duracionHoras\":1,\"acompaniantes\":[{\"nombre\":\"A\",\"apellido\":\"B\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));
    }

    @Test
    void registrar_erroresDelServicio_409Ocupado_422FueraDePlazo_403OtroTitular() throws Exception {
        when(reservaService.registrar(any(ReservaRequest.class), eq(SOCIO)))
                .thenThrow(new ConflictoException(CodigoError.RESERVA_HORARIO_OCUPADO, "Ese horario ya está reservado"))
                .thenThrow(new ReglaDeNegocioException(CodigoError.RESERVA_FUERA_DE_PLAZO, "Se puede reservar hasta con 30 días de antelación"))
                .thenThrow(new AccessDeniedException("Un socio solo reserva para sí mismo"));

        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RESERVA_HORARIO_OCUPADO"));
        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RESERVA_FUERA_DE_PLAZO"));
        mockMvc.perform(post("/api/reservas").header("Authorization", socio()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isForbidden());
    }

    // ---------- Consultas ----------

    @Test
    void misReservas_200_comoSocio_conFiltros() throws Exception {
        when(reservaService.misReservas(true, EstadoReserva.CONFIRMADA, 0, 20, SOCIO))
                .thenReturn(new PageResponse<>(List.of(reserva(10L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/reservas/mis-reservas?proximas=true&estado=CONFIRMADA").header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.contenido[0].id").value(10));
    }

    @Test
    void misReservas_conTamanioMayorAlTopeOEstadoInexistente_400() throws Exception {
        mockMvc.perform(get("/api/reservas/mis-reservas?size=51").header("Authorization", socio())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reservas/mis-reservas?estado=ROBADA").header("Authorization", socio()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
    }

    @Test
    void buscar_200_comoEmpleado_conTodosLosFiltros() throws Exception {
        when(reservaService.buscar("30123456", "R-000010", EstadoReserva.CONFIRMADA, LocalDate.of(2026, 10, 22), 1L, 0, 20))
                .thenReturn(new PageResponse<>(List.of(reserva(10L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/reservas?documento=30123456&numero=R-000010&estado=CONFIRMADA&fecha=2026-10-22&canchaId=1")
                        .header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_conNumeroMuyLargoOPaginaNegativa_400() throws Exception {
        mockMvc.perform(get("/api/reservas?numero=" + "9".repeat(31)).header("Authorization", empleado())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reservas?page=-1").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }

    @Test
    void consultar_200_para_socioYEmpleado_y_404SiNoEsSuya() throws Exception {
        when(reservaService.consultar(10L, SOCIO)).thenReturn(reserva(10L));
        when(reservaService.consultar(11L, SOCIO)).thenThrow(
                new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        when(reservaService.consultar(10L, EMPLEADO)).thenReturn(reserva(10L));

        mockMvc.perform(get("/api/reservas/10").header("Authorization", socio())).andExpect(status().isOk());
        mockMvc.perform(get("/api/reservas/10").header("Authorization", empleado())).andExpect(status().isOk());
        mockMvc.perform(get("/api/reservas/11").header("Authorization", socio()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RESERVA_NO_ENCONTRADA"));
    }

    @Test
    void consultar_conIdNoPositivoOQueNoEsNumero_400() throws Exception {
        mockMvc.perform(get("/api/reservas/0").header("Authorization", socio())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reservas/abc").header("Authorization", socio())).andExpect(status().isBadRequest());
    }

    // ---------- CU17 ----------

    @Test
    void modificar_200_comoEmpleado() throws Exception {
        when(reservaService.modificar(eq(10L), any(ReservaUpdateRequest.class), eq(EMPLEADO))).thenReturn(reserva(10L));

        mockMvc.perform(patch("/api/reservas/10").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"horaInicio\":\"19:00\",\"duracionHoras\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void modificar_conDuracionFueraDeRango_400() throws Exception {
        mockMvc.perform(patch("/api/reservas/10").header("Authorization", empleado()).contentType(JSON).content("{\"duracionHoras\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.duracionHoras").exists());
        verify(reservaService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_erroresDelServicio_409Ocupado_422NoModificable_404() throws Exception {
        when(reservaService.modificar(eq(10L), any(ReservaUpdateRequest.class), any())).thenThrow(
                new ConflictoException(CodigoError.RESERVA_HORARIO_BLOQUEADO, "La cancha está en mantenimiento en ese horario"));
        when(reservaService.modificar(eq(11L), any(ReservaUpdateRequest.class), any())).thenThrow(
                new ReglaDeNegocioException(CodigoError.RESERVA_NO_MODIFICABLE, "La reserva está CANCELADA: ya no se puede modificar"));
        when(reservaService.modificar(eq(99L), any(ReservaUpdateRequest.class), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));

        mockMvc.perform(patch("/api/reservas/10").header("Authorization", empleado()).contentType(JSON).content("{\"duracionHoras\":1}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/reservas/11").header("Authorization", empleado()).contentType(JSON).content("{\"duracionHoras\":1}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RESERVA_NO_MODIFICABLE"));
        mockMvc.perform(patch("/api/reservas/99").header("Authorization", empleado()).contentType(JSON).content("{\"duracionHoras\":1}"))
                .andExpect(status().isNotFound());
    }

    // ---------- CU18 ----------

    @Test
    void previsualizarCancelacion_200_paraSocioYEmpleado() throws Exception {
        when(cancelacionService.previsualizar(10L, SOCIO)).thenReturn(
                new CancelacionResponse(false, 3, new BigDecimal("8000.00"), BigDecimal.ZERO, "Cancelás con menos de 6 horas de antelación"));

        mockMvc.perform(get("/api/reservas/10/cancelacion").header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aTiempo").value(false))
                .andExpect(jsonPath("$.montoPenalidad").value(8000.00));
    }

    @Test
    void cancelar_200_conElResultado() throws Exception {
        when(cancelacionService.cancelar(10L, SOCIO)).thenReturn(reserva(10L));

        mockMvc.perform(post("/api/reservas/10/cancelacion").header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")));
        verify(cancelacionService).cancelar(10L, SOCIO);
    }

    @Test
    void cancelar_erroresDelServicio_403NoEsElTitular_404_422YaCancelada() throws Exception {
        when(cancelacionService.cancelar(eq(10L), any())).thenThrow(new AccessDeniedException("Solo el titular puede cancelar la reserva"));
        when(cancelacionService.cancelar(eq(11L), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        when(cancelacionService.cancelar(eq(12L), any())).thenThrow(
                new ReglaDeNegocioException(CodigoError.RESERVA_NO_CANCELABLE, "La reserva está CANCELADA: ya no se puede cancelar"));

        mockMvc.perform(post("/api/reservas/10/cancelacion").header("Authorization", socio())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/11/cancelacion").header("Authorization", socio())).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/reservas/12/cancelacion").header("Authorization", socio()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RESERVA_NO_CANCELABLE"));
    }

    // ---------- CU19 ----------

    @Test
    void disponibilidad_200_paraTodosLosRoles_conFiltros() throws Exception {
        when(disponibilidad.consultar(eq(LocalDate.of(2026, 10, 22)), eq(LocalTime.of(16, 0)), eq(LocalTime.of(21, 0)), any()))
                .thenReturn(new DisponibilidadReservaResponse(LocalDate.of(2026, 10, 22), true, null, List.of()));

        mockMvc.perform(get("/api/reservas/disponibilidad?fecha=2026-10-22&horaDesde=16:00&horaHasta=21:00&superficie=CEMENTO")
                        .header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservable").value(true));
        mockMvc.perform(get("/api/reservas/disponibilidad?fecha=2026-10-22&horaDesde=16:00&horaHasta=21:00")
                .header("Authorization", empleado())).andExpect(status().isOk());
    }

    @Test
    void disponibilidad_sinFechaOFechaMalEscrita_400() throws Exception {
        mockMvc.perform(get("/api/reservas/disponibilidad").header("Authorization", socio()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
        mockMvc.perform(get("/api/reservas/disponibilidad?fecha=hoy").header("Authorization", socio())).andExpect(status().isBadRequest());
    }

    @Test
    void disponibilidad_superficieSinCoincidencias_200ConListaVacia() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 10, 22);
        when(disponibilidad.consultar(fecha, null, null, "TIERRA"))
                .thenReturn(new DisponibilidadReservaResponse(fecha, true, null, List.of()));

        mockMvc.perform(get("/api/reservas/disponibilidad?fecha=2026-10-22&superficie=TIERRA").header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-10-22"))
                .andExpect(jsonPath("$.canchas").isEmpty());
    }
}
