package com.tenisahora.modules.reserva.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.AsignacionDeInsumosRequest;
import com.tenisahora.modules.reserva.dto.EstadoDeAsignacion;
import com.tenisahora.modules.reserva.dto.InsumoAsignadoResponse;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.service.CicloDelAlquiler;
import com.tenisahora.modules.reserva.service.InsumosDeReservaService;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.CodigoError;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({InsumosDeReservaController.class, CicloDelAlquilerController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class InsumosYCicloControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;
    private static final Actor SOCIO = new Actor(ID_SOCIO, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(ID_EMPLEADO, Rol.EMPLEADO);
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String PEDIDO = "{\"insumos\":[{\"insumoId\":1,\"cantidad\":4},{\"insumoId\":2,\"cantidad\":12}]}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InsumosDeReservaService insumosDeReserva;
    @MockitoBean
    private CicloDelAlquiler cicloDelAlquiler;
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

    private static ReservaResponse reserva(EstadoReserva estado) {
        return new ReservaResponse(10L, "R-000010", 1L, "Cancha 1", LocalDate.of(2026, 10, 22), LocalTime.of(18, 0), LocalTime.of(20, 0), estado,
                2, List.of(), null, null, null, null, null, null, null, null,
                List.of(new InsumoAsignadoResponse(1L, "Raquetas", 4, EstadoDeAsignacion.ASIGNADO)), null, null, null, null, null);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401_enTodosLosEndpoints() throws Exception {
        mockMvc.perform(put("/api/reservas/10/insumos").contentType(JSON).content(PEDIDO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservas/insumos-disponibles?fecha=2026-10-22&horaInicio=18:00&duracionHoras=2")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reservas/10/inicio")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reservas/10/finalizacion")).andExpect(status().isUnauthorized());
    }

    @Test
    void conRolProfesor_403_enTodosLosEndpoints() throws Exception {
        String profesor = bearer(ID_PROFESOR, Rol.PROFESOR);

        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", profesor).contentType(JSON).content(PEDIDO)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas/insumos-disponibles?fecha=2026-10-22&horaInicio=18:00&duracionHoras=2").header("Authorization", profesor))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/10/inicio").header("Authorization", profesor)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/10/finalizacion").header("Authorization", profesor)).andExpect(status().isForbidden());
    }

    @Test
    void unSocioNoIniciaNiFinalizaUnAlquiler_403_nuncaLlegaAlServicio() throws Exception {
        mockMvc.perform(post("/api/reservas/10/inicio").header("Authorization", socio()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));
        mockMvc.perform(post("/api/reservas/10/finalizacion").header("Authorization", socio())).andExpect(status().isForbidden());
        verify(cicloDelAlquiler, never()).iniciar(any(), any());
        verify(cicloDelAlquiler, never()).finalizar(any(), any());
    }

    // ---------- CU13 ----------

    @Test
    void asignar_200_paraElSocioYParaElEmpleado_conLosInsumosEnLaRespuesta() throws Exception {
        when(insumosDeReserva.asignar(eq(10L), any(AsignacionDeInsumosRequest.class), eq(SOCIO))).thenReturn(reserva(EstadoReserva.PENDIENTE_PAGO));
        when(insumosDeReserva.asignar(eq(10L), any(AsignacionDeInsumosRequest.class), eq(EMPLEADO))).thenReturn(reserva(EstadoReserva.CONFIRMADA));

        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON).content(PEDIDO))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.insumos[0].nombre").value("Raquetas"))
                .andExpect(jsonPath("$.insumos[0].estado").value("ASIGNADO"));
        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", empleado()).contentType(JSON).content(PEDIDO))
                .andExpect(status().isOk());
    }

    @Test
    void asignar_conUnaListaVacia_quitaTodo_200() throws Exception {
        when(insumosDeReserva.asignar(eq(10L), any(AsignacionDeInsumosRequest.class), eq(SOCIO))).thenReturn(reserva(EstadoReserva.PENDIENTE_PAGO));

        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON).content("{\"insumos\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void asignar_conDatosInvalidos_400() throws Exception {
        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.insumos").exists());
        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON)
                .content("{\"insumos\":[{\"insumoId\":1,\"cantidad\":0}]}")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON)
                .content("{\"insumos\":[{\"insumoId\":1,\"cantidad\":101}]}")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON)
                .content("{\"insumos\":[{\"cantidad\":2}]}")).andExpect(status().isBadRequest());
        verify(insumosDeReserva, never()).asignar(any(), any(), any());
    }

    @Test
    void asignar_conMasDeDiezRenglones_400() throws Exception {
        StringBuilder renglones = new StringBuilder();
        for (int i = 1; i <= 11; i++) {
            renglones.append(i > 1 ? "," : "").append("{\"insumoId\":").append(i).append(",\"cantidad\":1}");
        }

        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON).content("{\"insumos\":[" + renglones + "]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void asignar_erroresDelServicio_422SinStock_404Ajena_403NoEsElTitular() throws Exception {
        when(insumosDeReserva.asignar(eq(10L), any(AsignacionDeInsumosRequest.class), any()))
                .thenThrow(new ReglaDeNegocioException(CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay 3 unidades de Raquetas para ese horario y se pidieron 4"));
        when(insumosDeReserva.asignar(eq(11L), any(AsignacionDeInsumosRequest.class), any()))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));
        when(insumosDeReserva.asignar(eq(12L), any(AsignacionDeInsumosRequest.class), any())).thenThrow(new AccessDeniedException("Solo el titular"));

        mockMvc.perform(put("/api/reservas/10/insumos").header("Authorization", socio()).contentType(JSON).content(PEDIDO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("INSUMO_STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("Solo hay 3")));
        mockMvc.perform(put("/api/reservas/11/insumos").header("Authorization", socio()).contentType(JSON).content(PEDIDO)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/reservas/12/insumos").header("Authorization", socio()).contentType(JSON).content(PEDIDO)).andExpect(status().isForbidden());
    }

    @Test
    void insumosDisponibles_200_paraElSocio() throws Exception {
        when(insumosDeReserva.disponibles(LocalDate.of(2026, 10, 22), LocalTime.of(18, 0), 2))
                .thenReturn(List.of(new InsumoDisponibleResponse(1L, "Raquetas", new CategoriaInsumoResponse(1L, "Raqueta"), 10, 6)));

        mockMvc.perform(get("/api/reservas/insumos-disponibles?fecha=2026-10-22&horaInicio=18:00&duracionHoras=2").header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadLibre").value(10))
                .andExpect(jsonPath("$[0].cantidadAsignable").value(6));
    }

    @Test
    void insumosDisponibles_sinParametrosOConDuracionInvalida_400() throws Exception {
        mockMvc.perform(get("/api/reservas/insumos-disponibles").header("Authorization", socio()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
        mockMvc.perform(get("/api/reservas/insumos-disponibles?fecha=2026-10-22&horaInicio=18:00&duracionHoras=0").header("Authorization", socio()))
                .andExpect(status().isBadRequest());
        verify(insumosDeReserva, never()).disponibles(any(), any(), anyInt());
    }

    // ---------- CU14 ----------

    @Test
    void iniciar_200_comoEmpleadoYComoAdministrador() throws Exception {
        when(cicloDelAlquiler.iniciar(eq(10L), eq(EMPLEADO))).thenReturn(reserva(EstadoReserva.EN_CURSO));
        when(cicloDelAlquiler.iniciar(eq(10L), eq(new Actor(ID_ADMIN, Rol.ADMINISTRADOR)))).thenReturn(reserva(EstadoReserva.EN_CURSO));

        mockMvc.perform(post("/api/reservas/10/inicio").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
        mockMvc.perform(post("/api/reservas/10/inicio").header("Authorization", bearer(ID_ADMIN, Rol.ADMINISTRADOR))).andExpect(status().isOk());
    }

    @Test
    void iniciar_erroresDelServicio_422NoIniciable_422SinStock_404() throws Exception {
        when(cicloDelAlquiler.iniciar(eq(10L), any())).thenThrow(new ReglaDeNegocioException(CodigoError.RESERVA_NO_INICIABLE, "solo se inicia una reserva confirmada"));
        when(cicloDelAlquiler.iniciar(eq(11L), any())).thenThrow(new ReglaDeNegocioException(CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay 3 unidades disponibles de Pelotas"));
        when(cicloDelAlquiler.iniciar(eq(99L), any())).thenThrow(new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));

        mockMvc.perform(post("/api/reservas/10/inicio").header("Authorization", empleado()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RESERVA_NO_INICIABLE"));
        mockMvc.perform(post("/api/reservas/11/inicio").header("Authorization", empleado()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("INSUMO_STOCK_INSUFICIENTE"));
        mockMvc.perform(post("/api/reservas/99/inicio").header("Authorization", empleado())).andExpect(status().isNotFound());
    }

    // ---------- CU15 ----------

    @Test
    void finalizar_200_comoEmpleado_y_422SiNoEstaEnCurso() throws Exception {
        when(cicloDelAlquiler.finalizar(eq(10L), eq(EMPLEADO))).thenReturn(reserva(EstadoReserva.FINALIZADA));
        when(cicloDelAlquiler.finalizar(eq(11L), any())).thenThrow(new ReglaDeNegocioException(CodigoError.RESERVA_NO_FINALIZABLE, "solo se finaliza un alquiler en curso"));

        mockMvc.perform(post("/api/reservas/10/finalizacion").header("Authorization", empleado()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FINALIZADA"));
        mockMvc.perform(post("/api/reservas/11/finalizacion").header("Authorization", empleado()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RESERVA_NO_FINALIZABLE"));
    }

    @Test
    void iniciarYFinalizar_conIdNoPositivoOQueNoEsNumero_400() throws Exception {
        mockMvc.perform(post("/api/reservas/0/inicio").header("Authorization", empleado())).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/reservas/abc/finalizacion").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }
}
