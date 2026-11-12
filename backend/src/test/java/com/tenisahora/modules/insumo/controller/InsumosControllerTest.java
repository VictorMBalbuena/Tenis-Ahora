package com.tenisahora.modules.insumo.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.dto.MovimientoRequest;
import com.tenisahora.modules.insumo.dto.ResumenInsumosResponse;
import com.tenisahora.modules.insumo.dto.MovimientoResponse;
import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.modules.insumo.service.InsumoService;
import com.tenisahora.modules.insumo.service.InsumoStock;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({InsumoController.class, MovimientoStockController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class InsumosControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final long ID_PROFESOR = 4L;

    private static final String ALTA_VALIDA = """
            {"nombre":"Pelotas de entrenamiento","categoriaId":1,"cantidadDisponible":40,"cantidadMinima":10}
            """;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InsumoService insumoService;
    @MockitoBean
    private InsumoStock insumoStock;
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

    private static InsumoResponse insumo(Long id) {
        return new InsumoResponse(id, "Pelotas de entrenamiento", new CategoriaInsumoResponse(1L, "Pelota"), 40, 10, 0, EstadoStock.DISPONIBLE);
    }

    // ---------- Permisos ----------

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/api/insumos")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mockMvc.perform(post("/api/insumos/1/movimientos").contentType(JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void conRolSocioOProfesor_403_enTodosLosEndpoints() throws Exception {
        for (String token : List.of(bearer(ID_SOCIO, Rol.SOCIO), bearer(ID_PROFESOR, Rol.PROFESOR))) {
            mockMvc.perform(get("/api/insumos").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/insumos/1").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/insumos").header("Authorization", token).contentType(JSON).content(ALTA_VALIDA))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/insumos/1").header("Authorization", token).contentType(JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/insumos/1").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/insumos/1/movimientos").header("Authorization", token).contentType(JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/insumos/1/movimientos").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void conRolSocio_y_unCuerpoInvalido_403_nuncaSeLlegaAValidar() throws Exception {
        mockMvc.perform(post("/api/insumos").header("Authorization", bearer(ID_SOCIO, Rol.SOCIO)).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.campos").doesNotExist());
        verify(insumoService, never()).registrar(any(), any());
    }

    // ---------- CU10 ----------

    @Test
    void registrar_201_conLocation() throws Exception {
        when(insumoService.registrar(any(InsumoRequest.class), eq(ID_EMPLEADO))).thenReturn(insumo(12L));

        mockMvc.perform(post("/api/insumos").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/insumos/12")))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void registrar_unAdministradorTambienPuede() throws Exception {
        when(insumoService.registrar(any(InsumoRequest.class), eq(ID_ADMIN))).thenReturn(insumo(12L));

        mockMvc.perform(post("/api/insumos").header("Authorization", bearer(ID_ADMIN, Rol.ADMINISTRADOR)).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated());
    }

    @Test
    void registrar_sinDatos_400ConElDetalleDeCadaCampo() throws Exception {
        mockMvc.perform(post("/api/insumos").header("Authorization", empleado()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.categoriaId").exists())
                .andExpect(jsonPath("$.campos.cantidadDisponible").exists())
                .andExpect(jsonPath("$.campos.cantidadMinima").exists());
        verify(insumoService, never()).registrar(any(), any());
    }

    @Test
    void registrar_conCantidadNegativa_400_A1() throws Exception {
        mockMvc.perform(post("/api/insumos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"nombre\":\"X\",\"categoriaId\":1,\"cantidadDisponible\":-1,\"cantidadMinima\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidadDisponible").exists());
    }

    @Test
    void registrar_conCategoriaIdMalFormado_400() throws Exception {
        mockMvc.perform(post("/api/insumos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"nombre\":\"X\",\"categoriaId\":\"GRIP\",\"cantidadDisponible\":1,\"cantidadMinima\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
    }

    @Test
    void registrar_ignoraId_activoYOtrosCamposQueNoSonDelAlta() throws Exception {
        when(insumoService.registrar(any(InsumoRequest.class), eq(ID_EMPLEADO))).thenReturn(insumo(12L));

        mockMvc.perform(post("/api/insumos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"id\":99,\"activo\":false,\"nombre\":\"X\",\"categoriaId\":1,\"cantidadDisponible\":1,\"cantidadMinima\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void registrar_nombreRepetido_409() throws Exception {
        when(insumoService.registrar(any(InsumoRequest.class), any())).thenThrow(
                new ConflictoException(CodigoError.INSUMO_NOMBRE_DUPLICADO, "Ya existe un insumo con ese nombre"));

        mockMvc.perform(post("/api/insumos").header("Authorization", empleado()).contentType(JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSUMO_NOMBRE_DUPLICADO"));
    }

    // ---------- CU102 y CU11 ----------

    @Test
    void buscar_200ConLaPaginaYLosFiltros() throws Exception {
        when(insumoService.buscar("pelota", 1L, EstadoStock.STOCK_BAJO, 0, 20))
                .thenReturn(new PageResponse<>(List.of(insumo(1L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/insumos?busqueda=pelota&categoriaId=1&estado=STOCK_BAJO").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_conTamanioMayorAlTopeOPaginaNegativa_400() throws Exception {
        mockMvc.perform(get("/api/insumos?size=51").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        mockMvc.perform(get("/api/insumos?page=-1").header("Authorization", empleado())).andExpect(status().isBadRequest());
        verify(insumoService, never()).buscar(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void buscar_conCategoriaIdOEstadoInexistente_400() throws Exception {
        mockMvc.perform(get("/api/insumos?categoriaId=GRIP").header("Authorization", empleado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_MAL_FORMADO"));
        mockMvc.perform(get("/api/insumos?estado=ROTO").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }

    @Test
    void consultar_200_y_inexistente_404ConElMensajeDelCasoDeUso_A1() throws Exception {
        when(insumoService.consultar(1L)).thenReturn(insumo(1L));
        when(insumoService.consultar(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado"));

        mockMvc.perform(get("/api/insumos/1").header("Authorization", empleado())).andExpect(status().isOk())
                .andExpect(jsonPath("$.enUso").value(0));
        mockMvc.perform(get("/api/insumos/99").header("Authorization", empleado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("INSUMO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Insumo no encontrado"));
    }

    @Test
    void consultar_conIdNoPositivoOQueNoEsNumero_400() throws Exception {
        mockMvc.perform(get("/api/insumos/0").header("Authorization", empleado())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/insumos/abc").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }

    @Test
    void resumen_200ConLosTotalesGlobales() throws Exception {
        when(insumoService.resumen()).thenReturn(new ResumenInsumosResponse(12, 340, 18, 2));

        mockMvc.perform(get("/api/insumos/resumen").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distintos").value(12))
                .andExpect(jsonPath("$.enDeposito").value(340))
                .andExpect(jsonPath("$.asignadas").value(18))
                .andExpect(jsonPath("$.alertas").value(2));
    }

    // ---------- CU12 ----------

    @Test
    void modificar_200() throws Exception {
        when(insumoService.modificar(eq(1L), any(InsumoUpdateRequest.class), eq(ID_EMPLEADO))).thenReturn(insumo(1L));

        mockMvc.perform(patch("/api/insumos/1").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"cantidadMinima\":12,\"cantidadDisponible\":38}"))
                .andExpect(status().isOk());
    }

    @Test
    void modificar_conCantidadPorDebajoDeCero_400_A1() throws Exception {
        mockMvc.perform(patch("/api/insumos/1").header("Authorization", empleado()).contentType(JSON).content("{\"cantidadDisponible\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidadDisponible").exists());
        verify(insumoService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_conNombreEnBlanco_400() throws Exception {
        mockMvc.perform(patch("/api/insumos/1").header("Authorization", empleado()).contentType(JSON).content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    void modificar_nombreDeOtro_409_e_inexistente_404() throws Exception {
        when(insumoService.modificar(eq(1L), any(InsumoUpdateRequest.class), any())).thenThrow(
                new ConflictoException(CodigoError.INSUMO_NOMBRE_DUPLICADO, "Ya existe un insumo con ese nombre"));
        when(insumoService.modificar(eq(99L), any(InsumoUpdateRequest.class), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado"));

        mockMvc.perform(patch("/api/insumos/1").header("Authorization", empleado()).contentType(JSON).content("{\"nombre\":\"Redes\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/insumos/99").header("Authorization", empleado()).contentType(JSON).content("{\"cantidadMinima\":1}"))
                .andExpect(status().isNotFound());
    }

    // ---------- CU101 ----------

    @Test
    void eliminar_204() throws Exception {
        mockMvc.perform(delete("/api/insumos/1").header("Authorization", empleado())).andExpect(status().isNoContent());

        verify(insumoService).eliminar(1L, ID_EMPLEADO);
    }

    @Test
    void eliminar_conReservasVigentes_409_inexistente_404() throws Exception {
        doThrow(new ConflictoException(CodigoError.INSUMO_CON_RESERVAS,
                "Hay 1 reserva(s) vigente(s) con este insumo (ids: 4). Cancelalas o reprogramalas antes de darlo de baja"))
                .when(insumoService).eliminar(eq(1L), any());
        doThrow(new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado"))
                .when(insumoService).eliminar(eq(99L), any());

        mockMvc.perform(delete("/api/insumos/1").header("Authorization", empleado()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSUMO_CON_RESERVAS"));
        mockMvc.perform(delete("/api/insumos/99").header("Authorization", empleado())).andExpect(status().isNotFound());
    }

    // ---------- CU14 y CU15 ----------

    @Test
    void mover_201ConElInsumoActualizado() throws Exception {
        when(insumoStock.mover(eq(1L), any(MovimientoRequest.class), eq(ID_EMPLEADO))).thenReturn(insumo(1L));

        mockMvc.perform(post("/api/insumos/1/movimientos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"tipo\":\"DESCUENTO\",\"cantidad\":4,\"motivo\":\"Rotura\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadDisponible").value(40));
    }

    @Test
    void mover_descontandoMasDeLoQueHay_422YElCodigoDelCriterioDeAceptacion() throws Exception {
        when(insumoStock.mover(eq(1L), any(MovimientoRequest.class), any())).thenThrow(new ReglaDeNegocioException(
                CodigoError.INSUMO_STOCK_INSUFICIENTE, "Solo hay 3 unidades disponibles de Raquetas y se pidieron 30"));

        mockMvc.perform(post("/api/insumos/1/movimientos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"tipo\":\"DESCUENTO\",\"cantidad\":30}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("INSUMO_STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("Solo hay 3")));
    }

    @Test
    void mover_conDatosInvalidos_400() throws Exception {
        mockMvc.perform(post("/api/insumos/1/movimientos").header("Authorization", empleado()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").exists())
                .andExpect(jsonPath("$.campos.cantidad").exists());
        mockMvc.perform(post("/api/insumos/1/movimientos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"tipo\":\"REPOSICION\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").exists());
        mockMvc.perform(post("/api/insumos/1/movimientos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"tipo\":\"REPOSICION\",\"cantidad\":100001}"))
                .andExpect(status().isBadRequest());
        verify(insumoStock, never()).mover(any(), any(), any());
    }

    @Test
    void mover_insumoInexistente_404() throws Exception {
        when(insumoStock.mover(eq(99L), any(MovimientoRequest.class), any())).thenThrow(
                new RecursoNoEncontradoException(CodigoError.INSUMO_NO_ENCONTRADO, "Insumo no encontrado"));

        mockMvc.perform(post("/api/insumos/99/movimientos").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"tipo\":\"REPOSICION\",\"cantidad\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void historial_200_y_tamanioMayorAlTope_400() throws Exception {
        when(insumoStock.historial(1L, 0, 20)).thenReturn(new PageResponse<>(List.of(new MovimientoResponse(
                15L, TipoMovimiento.DESCUENTO, -4, 20, "Rotura", LocalDateTime.of(2026, 10, 19, 10, 0), ID_EMPLEADO)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/insumos/1/movimientos").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].variacion").value(-4))
                .andExpect(jsonPath("$.contenido[0].tipo").value("DESCUENTO"));
        mockMvc.perform(get("/api/insumos/1/movimientos?size=51").header("Authorization", empleado())).andExpect(status().isBadRequest());
    }
}
