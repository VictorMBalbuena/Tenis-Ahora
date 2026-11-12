package com.tenisahora.modules.insumo.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.service.CategoriaInsumoService;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
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

@WebMvcTest(CategoriaInsumoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class CategoriaInsumoControllerTest {

    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CategoriaInsumoService categoriaInsumoService;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), any())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), any())).thenReturn(Optional.of("SOCIO"));
    }

    private String empleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, Rol.EMPLEADO.name());
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, Rol.SOCIO.name());
    }

    @Test
    void listar_200ConLasCategorias() throws Exception {
        when(categoriaInsumoService.listar()).thenReturn(List.of(new CategoriaInsumoResponse(1L, "Raqueta")));

        mockMvc.perform(get("/api/categorias-insumo").header("Authorization", empleado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Raqueta"));
    }

    @Test
    void listar_sinPermiso_403() throws Exception {
        mockMvc.perform(get("/api/categorias-insumo").header("Authorization", socio()))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrar_201ConLaCategoriaCreada() throws Exception {
        when(categoriaInsumoService.registrar(any())).thenReturn(new CategoriaInsumoResponse(4L, "Grip / overgrip"));

        mockMvc.perform(post("/api/categorias-insumo").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"nombre\":\"Grip / overgrip\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void registrar_sinNombre_400() throws Exception {
        mockMvc.perform(post("/api/categorias-insumo").header("Authorization", empleado()).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        verify(categoriaInsumoService, never()).registrar(any());
    }

    @Test
    void registrar_nombreRepetido_409() throws Exception {
        when(categoriaInsumoService.registrar(any())).thenThrow(
                new ConflictoException(CodigoError.CATEGORIA_INSUMO_NOMBRE_DUPLICADO, "Ya existe una categoría con ese nombre"));

        mockMvc.perform(post("/api/categorias-insumo").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"nombre\":\"Raqueta\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_INSUMO_NOMBRE_DUPLICADO"));
    }

    @Test
    void modificar_200ConElNombreNuevo() throws Exception {
        when(categoriaInsumoService.modificar(eq(1L), any())).thenReturn(new CategoriaInsumoResponse(1L, "Raquetas"));

        mockMvc.perform(patch("/api/categorias-insumo/1").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"nombre\":\"Raquetas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Raquetas"));
    }

    @Test
    void modificar_nombreDeOtraCategoria_409() throws Exception {
        when(categoriaInsumoService.modificar(eq(1L), any())).thenThrow(
                new ConflictoException(CodigoError.CATEGORIA_INSUMO_NOMBRE_DUPLICADO, "Ya existe una categoría con ese nombre"));

        mockMvc.perform(patch("/api/categorias-insumo/1").header("Authorization", empleado()).contentType(JSON)
                        .content("{\"nombre\":\"Pelota\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminar_204() throws Exception {
        mockMvc.perform(delete("/api/categorias-insumo/1").header("Authorization", empleado()))
                .andExpect(status().isNoContent());
        verify(categoriaInsumoService).eliminar(1L);
    }

    @Test
    void eliminar_conInsumosAsignados_409() throws Exception {
        doThrow(new ConflictoException(CodigoError.CATEGORIA_INSUMO_CON_INSUMOS, "Hay 3 insumo(s) con esta categoría. Reasignalos antes de eliminarla"))
                .when(categoriaInsumoService).eliminar(1L);

        mockMvc.perform(delete("/api/categorias-insumo/1").header("Authorization", empleado()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_INSUMO_CON_INSUMOS"));
    }

    @Test
    void eliminar_sinPermiso_403() throws Exception {
        mockMvc.perform(delete("/api/categorias-insumo/1").header("Authorization", socio()))
                .andExpect(status().isForbidden());
        verify(categoriaInsumoService, never()).eliminar(any());
    }
}
