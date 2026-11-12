package com.tenisahora.modules.cancha.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.cancha.dto.BloqueoRequest;
import com.tenisahora.modules.cancha.dto.BloqueoResponse;
import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.dto.EstadoFranja;
import com.tenisahora.modules.cancha.dto.FranjaResponse;
import com.tenisahora.modules.cancha.dto.GrillaResponse;
import com.tenisahora.modules.cancha.dto.HorarioRequest;
import com.tenisahora.modules.cancha.dto.HorarioResponse;
import com.tenisahora.modules.cancha.service.BloqueoService;
import com.tenisahora.modules.cancha.service.CanchaService;
import com.tenisahora.modules.cancha.service.DisponibilidadService;
import com.tenisahora.modules.cancha.service.HorarioService;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.tenisahora.modules.cancha.dto.BloqueoDelDiaResponse;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest({CanchaController.class, HorarioController.class, BloqueoController.class, DisponibilidadController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class CanchasControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;

    private static final String ALTA_VALIDA = """
            {"identificador":"Cancha 3","tipoSuperficie":"PASTO","capacidad":4,"precioPorHora":7000.00}
            """;
    private static final String HORARIO_VALIDO = """
            {"dias":["MONDAY","TUESDAY"],"desde":"08:00","hasta":"12:00"}
            """;
    private static final String BLOQUEO_VALIDO = """
            {"fechaDesde":"2026-10-20","fechaHasta":"2026-10-22","horaDesde":"08:00","horaHasta":"18:00","motivo":"Cambio de superficie"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CanchaService canchaService;
    @MockitoBean
    private HorarioService horarioService;
    @MockitoBean
    private BloqueoService bloqueoService;
    @MockitoBean
    private DisponibilidadService disponibilidadService;
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

    private String empleado() {
        return bearer(ID_EMPLEADO, Rol.EMPLEADO);
    }

    private static CanchaResponse cancha(Long id) {
        return new CanchaResponse(id, "Cancha 3", "PASTO", 4, new BigDecimal("7000.00"), true, false);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(get("/api/canchas")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mockMvc.perform(get("/api/canchas/1/disponibilidad?desde=2026-10-20&hasta=2026-10-21")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/canchas/1/bloqueos").contentType(MediaType.APPLICATION_JSON).content(BLOQUEO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conRolSocio_403_enElAbmLosHorariosYLosBloqueos() throws Exception {
        String socio = bearer(ID_SOCIO, Rol.SOCIO);

        mockMvc.perform(get("/api/canchas").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/canchas/1").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/canchas").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/canchas/1").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/canchas/1/horarios").header("Authorization", socio)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/canchas/1/horarios").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content(HORARIO_VALIDO)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/canchas/1/bloqueos").header("Authorization", socio)
                .contentType(MediaType.APPLICATION_JSON).content(BLOQUEO_VALIDO)).andExpect(status().isForbidden());
    }

    @Test
    void conRolSocio_y_unCuerpoInvalido_403_nuncaSeLlegaAValidar() throws Exception {
        mockMvc.perform(post("/api/canchas").header("Authorization", bearer(ID_SOCIO, Rol.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.campos").doesNotExist());
        verify(canchaService, never()).registrar(any(), any());
    }

    @Test
    void conRolProfesor_403_tambienEnLaDisponibilidad() throws Exception {
        mockMvc.perform(get("/api/canchas/1/disponibilidad?desde=2026-10-20&hasta=2026-10-21")
                        .header("Authorization", bearer(ID_PROFESOR, Rol.PROFESOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));
        verify(disponibilidadService, never()).consultar(any(), any(), any());
    }

    @Test
    void conRolSocio_puedeConsultarLaDisponibilidad() throws Exception {
        when(disponibilidadService.consultar(1L, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)))
                .thenReturn(new GrillaResponse(1L, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21), List.of()));

        mockMvc.perform(get("/api/canchas/1/disponibilidad?desde=2026-10-20&hasta=2026-10-21")
                        .header("Authorization", bearer(ID_SOCIO, Rol.SOCIO)))
                .andExpect(status().isOk());
    }

    // ---------- CU06 ----------

    @Test
    void registrar_201_conLocation() throws Exception {
        when(canchaService.registrar(any(CanchaRequest.class), eq(ID_EMPLEADO))).thenReturn(cancha(12L));

        mockMvc.perform(post("/api/canchas").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/canchas/12")))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.identificador").value("Cancha 3"));
    }

    @Test
    void registrar_unAdministradorTambienPuede() throws Exception {
        when(canchaService.registrar(any(CanchaRequest.class), eq(ID_ADMIN))).thenReturn(cancha(12L));

        mockMvc.perform(post("/api/canchas").header("Authorization", bearer(ID_ADMIN, Rol.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated());
    }

    @Test
    void registrar_sinDatosObligatorios_400ConElDetalleDeCadaCampo_A1() throws Exception {
        mockMvc.perform(post("/api/canchas").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.identificador").exists())
                .andExpect(jsonPath("$.campos.tipoSuperficie").exists())
                .andExpect(jsonPath("$.campos.capacidad").exists())
                .andExpect(jsonPath("$.campos.precioPorHora").exists());
        verify(canchaService, never()).registrar(any(), any());
    }

    @Test
    void registrar_conCapacidadFueraDeRangoOPrecioNegativo_400() throws Exception {
        mockMvc.perform(post("/api/canchas").header("Authorization", empleado()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"C\",\"tipoSuperficie\":\"PASTO\",\"capacidad\":5,\"precioPorHora\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.capacidad").exists())
                .andExpect(jsonPath("$.campos.precioPorHora").exists());
    }

    @Test
    void registrar_conSuperficieVacia_400() throws Exception {
        mockMvc.perform(post("/api/canchas").header("Authorization", empleado()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"C\",\"tipoSuperficie\":\" \",\"capacidad\":4,\"precioPorHora\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipoSuperficie").exists());
    }

    @Test
    void registrar_ignoraCamposQueNoSonDelAlta_idYActiva() throws Exception {
        when(canchaService.registrar(any(CanchaRequest.class), eq(ID_EMPLEADO))).thenReturn(cancha(12L));

        mockMvc.perform(post("/api/canchas").header("Authorization", empleado()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99,\"activa\":false,\"identificador\":\"Cancha 3\",\"tipoSuperficie\":\"PASTO\",\"capacidad\":4,\"precioPorHora\":7000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void registrar_identificadorRepetido_409() throws Exception {
        when(canchaService.registrar(any(CanchaRequest.class), any())).thenThrow(
                new ConflictoException(CodigoError.CANCHA_IDENTIFICADOR_DUPLICADO, "Ya existe una cancha con ese identificador"));

        mockMvc.perform(post("/api/canchas").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CANCHA_IDENTIFICADOR_DUPLICADO"));
    }

    // ---------- CU08 ----------

    @Test
    void buscar_200ConLaPagina() throws Exception {
        when(canchaService.buscar("PASTO", "can", null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(cancha(1L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/canchas?superficie=PASTO&identificador=can").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_conTamanioMayorAlTope_400() throws Exception {
        mockMvc.perform(get("/api/canchas?size=51").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(canchaService, never()).buscar(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void buscar_conPaginaNegativa_400() throws Exception {
        mockMvc.perform(get("/api/canchas?page=-1").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }

    @Test
    void buscar_conSuperficieTexto_200() throws Exception {
        when(canchaService.buscar("TIERRA", null, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/canchas?superficie=TIERRA").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    void consultar_200() throws Exception {
        when(canchaService.consultar(1L)).thenReturn(cancha(1L));

        mockMvc.perform(get("/api/canchas/1").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enMantenimiento").value(false));
    }

    @Test
    void consultar_inexistente_404ConElMensajeDelCasoDeUso_A1() throws Exception {
        when(canchaService.consultar(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));

        mockMvc.perform(get("/api/canchas/99").header("Authorization", empleado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CANCHA_NO_ENCONTRADA"))
                .andExpect(jsonPath("$.mensaje").value("Cancha no encontrada"));
    }

    @Test
    void consultar_conIdNoPositivo_400() throws Exception {
        mockMvc.perform(get("/api/canchas/0").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }

    // ---------- CU07 ----------

    @Test
    void modificar_200() throws Exception {
        when(canchaService.modificar(eq(1L), any(CanchaUpdateRequest.class), eq(ID_EMPLEADO))).thenReturn(cancha(1L));

        mockMvc.perform(patch("/api/canchas/1").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"capacidad\":2,\"activa\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void modificar_conIdentificadorEnBlanco_oCapacidadInvalida_400_A1() throws Exception {
        mockMvc.perform(patch("/api/canchas/1").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identificador\":\"   \",\"capacidad\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.identificador").exists())
                .andExpect(jsonPath("$.campos.capacidad").exists());
        verify(canchaService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_identificadorDeOtraCancha_409() throws Exception {
        when(canchaService.modificar(eq(1L), any(CanchaUpdateRequest.class), any())).thenThrow(
                new ConflictoException(CodigoError.CANCHA_IDENTIFICADOR_DUPLICADO, "Ya existe una cancha con ese identificador"));

        mockMvc.perform(patch("/api/canchas/1").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identificador\":\"Cancha 2\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void modificar_inexistente_404() throws Exception {
        when(canchaService.modificar(eq(99L), any(CanchaUpdateRequest.class), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));

        mockMvc.perform(patch("/api/canchas/99").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"capacidad\":2}"))
                .andExpect(status().isNotFound());
    }

    // ---------- CU090 ----------

    @Test
    void registrarHorarios_201() throws Exception {
        when(horarioService.registrar(eq(1L), any(HorarioRequest.class), eq(ID_EMPLEADO))).thenReturn(List.of(
                new HorarioResponse(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new HorarioResponse(2L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))));

        mockMvc.perform(post("/api/canchas/1/horarios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(HORARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dia").value("MONDAY"))
                .andExpect(jsonPath("$[0].desde").value("08:00:00"));
    }

    @Test
    void registrarHorarios_sinDiasOConDiaInexistente_400() throws Exception {
        mockMvc.perform(post("/api/canchas/1/horarios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":[],\"desde\":\"08:00\",\"hasta\":\"12:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dias").exists());
        mockMvc.perform(post("/api/canchas/1/horarios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dias\":[\"FUNDAY\"],\"desde\":\"08:00\",\"hasta\":\"12:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
        verify(horarioService, never()).registrar(any(), any(), any());
    }

    @Test
    void registrarHorarios_horarioSolapado_409_A1() throws Exception {
        when(horarioService.registrar(eq(1L), any(HorarioRequest.class), any())).thenThrow(
                new ConflictoException(CodigoError.HORARIO_SUPERPUESTO, "El horario se superpone con uno ya registrado el día MONDAY"));

        mockMvc.perform(post("/api/canchas/1/horarios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(HORARIO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("HORARIO_SUPERPUESTO"));
    }

    @Test
    void registrarHorarios_canchaDeshabilitada_422() throws Exception {
        when(horarioService.registrar(eq(1L), any(HorarioRequest.class), any())).thenThrow(
                new ReglaDeNegocioException(CodigoError.CANCHA_INACTIVA, "La cancha está deshabilitada"));

        mockMvc.perform(post("/api/canchas/1/horarios").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(HORARIO_VALIDO))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listarHorarios_200_y_canchaInexistente_404() throws Exception {
        when(horarioService.listar(1L)).thenReturn(List.of());
        when(horarioService.listar(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));

        mockMvc.perform(get("/api/canchas/1/horarios").header("Authorization", empleado())).andExpect(status().isOk());
        mockMvc.perform(get("/api/canchas/99/horarios").header("Authorization", empleado())).andExpect(status().isNotFound());
    }

    // ---------- CU091 ----------

    @Test
    void bloquear_201() throws Exception {
        when(bloqueoService.bloquear(eq(1L), any(BloqueoRequest.class), eq(ID_EMPLEADO))).thenReturn(new BloqueoResponse(
                1L, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), LocalTime.of(8, 0), LocalTime.of(18, 0), "Cambio de superficie", 3));

        mockMvc.perform(post("/api/canchas/1/bloqueos").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(BLOQUEO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dias").value(3))
                .andExpect(jsonPath("$.motivo").value("Cambio de superficie"));
    }

    @Test
    void bloquear_sinMotivoNiFechas_400() throws Exception {
        mockMvc.perform(post("/api/canchas/1/bloqueos").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"horaDesde\":\"08:00\",\"horaHasta\":\"18:00\",\"motivo\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").exists())
                .andExpect(jsonPath("$.campos.fechaDesde").exists())
                .andExpect(jsonPath("$.campos.fechaHasta").exists());
        verify(bloqueoService, never()).bloquear(any(), any(), any());
    }

    @Test
    void bloquear_conReservasConfirmadas_409ConSuMensaje_A1() throws Exception {
        when(bloqueoService.bloquear(eq(1L), any(BloqueoRequest.class), any())).thenThrow(new ConflictoException(
                CodigoError.BLOQUEO_CON_RESERVAS, "Hay 2 reserva(s) vigente(s) en ese rango (ids: 3, 5). Cancelalas o reprogramalas antes de bloquear la cancha"));

        mockMvc.perform(post("/api/canchas/1/bloqueos").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(BLOQUEO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("BLOQUEO_CON_RESERVAS"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("3, 5")));
    }

    @Test
    void bloquear_canchaInexistente_404() throws Exception {
        when(bloqueoService.bloquear(eq(99L), any(BloqueoRequest.class), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));

        mockMvc.perform(post("/api/canchas/99/bloqueos").header("Authorization", empleado())
                        .contentType(MediaType.APPLICATION_JSON).content(BLOQUEO_VALIDO))
                .andExpect(status().isNotFound());
    }

    // ---------- CU092 ----------

    @Test
    void disponibilidad_200ConLaGrilla() throws Exception {
        LocalDate dia = LocalDate.of(2026, 10, 20);
        when(disponibilidadService.consultar(1L, dia, dia)).thenReturn(new GrillaResponse(1L, dia, dia, List.of(
                new FranjaResponse(dia, LocalTime.of(8, 0), LocalTime.of(9, 0), EstadoFranja.BLOQUEADA))));

        mockMvc.perform(get("/api/canchas/1/disponibilidad?desde=2026-10-20&hasta=2026-10-20").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas[0].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.franjas[0].desde").value("08:00:00"));
    }

    @Test
    void disponibilidad_sinFechasOConFechaMalEscrita_400() throws Exception {
        mockMvc.perform(get("/api/canchas/1/disponibilidad").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
        mockMvc.perform(get("/api/canchas/1/disponibilidad?desde=20-10-2026&hasta=2026-10-21").header("Authorization", empleado()))
                .andExpect(status().isBadRequest());
        verify(disponibilidadService, never()).consultar(any(), any(), any());
    }

    @Test
    void disponibilidad_canchaInexistente_404() throws Exception {
        when(disponibilidadService.consultar(eq(99L), any(), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));

        mockMvc.perform(get("/api/canchas/99/disponibilidad?desde=2026-10-20&hasta=2026-10-21").header("Authorization", empleado()))
                .andExpect(status().isNotFound());
    }

    // ---------- Baja de horarios y levantamiento de bloqueos ----------

    @Test
    void eliminarHorario_204_yConRolSocio_403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/canchas/1/horarios/5")
                .header("Authorization", empleado())).andExpect(status().isNoContent());
        verify(horarioService).eliminar(1L, 5L, ID_EMPLEADO);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/canchas/1/horarios/5")
                .header("Authorization", bearer(ID_SOCIO, Rol.SOCIO))).andExpect(status().isForbidden());
    }

    @Test
    void eliminarHorario_inexistente_404() throws Exception {
        org.mockito.Mockito.doThrow(new RecursoNoEncontradoException(CodigoError.HORARIO_NO_ENCONTRADO, "Horario no encontrado"))
                .when(horarioService).eliminar(eq(1L), eq(99L), any());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/canchas/1/horarios/99")
                        .header("Authorization", empleado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("HORARIO_NO_ENCONTRADO"));
    }

    @Test
    void listarBloqueos_200() throws Exception {
        LocalDate dia = LocalDate.of(2026, 10, 20);
        when(bloqueoService.listar(1L, dia, dia.plusDays(3))).thenReturn(List.of(
                new BloqueoDelDiaResponse(7L, dia, LocalTime.of(8, 0), LocalTime.of(18, 0), "Riego")));

        mockMvc.perform(get("/api/canchas/1/bloqueos?desde=2026-10-20&hasta=2026-10-23").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].motivo").value("Riego"));
    }

    @Test
    void listarBloqueos_sinFechas_400_yConRolSocio_403() throws Exception {
        mockMvc.perform(get("/api/canchas/1/bloqueos").header("Authorization", empleado())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/canchas/1/bloqueos?desde=2026-10-20&hasta=2026-10-23")
                .header("Authorization", bearer(ID_SOCIO, Rol.SOCIO))).andExpect(status().isForbidden());
    }

    @Test
    void desbloquear_204_yBloqueoInexistente_404() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/canchas/1/bloqueos/7")
                .header("Authorization", empleado())).andExpect(status().isNoContent());
        verify(bloqueoService).desbloquear(1L, 7L, ID_EMPLEADO);

        org.mockito.Mockito.doThrow(new RecursoNoEncontradoException(CodigoError.BLOQUEO_NO_ENCONTRADO, "Bloqueo no encontrado"))
                .when(bloqueoService).desbloquear(eq(1L), eq(99L), any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/canchas/1/bloqueos/99")
                .header("Authorization", empleado())).andExpect(status().isNotFound());
    }

    @Test
    void eliminarHorario_conReservasVigentes_409_primeroSeCancelan() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictoException(CodigoError.HORARIO_CON_RESERVAS,
                "Hay 1 reserva(s) vigente(s) en ese horario (ids: 4). Cancelalas o reprogramalas antes de eliminarlo"))
                .when(horarioService).eliminar(eq(1L), eq(5L), any());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/canchas/1/horarios/5")
                        .header("Authorization", empleado()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("HORARIO_CON_RESERVAS"));
    }
}
