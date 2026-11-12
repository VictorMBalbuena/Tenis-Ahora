package com.tenisahora.modules.descuento.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.descuento.dto.DescuentoRequest;
import com.tenisahora.modules.descuento.dto.DescuentoResponse;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.service.DescuentoService;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DescuentoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class DescuentoControllerTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_EMPLEADO = 2L;
    private static final long ID_SOCIO = 3L;
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DescuentoService descuentoService;

    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;

    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        when(verificadorDeSesion.rolVigente(eq(ID_ADMIN), any())).thenReturn(Optional.of("ADMINISTRADOR"));
        when(verificadorDeSesion.rolVigente(eq(ID_EMPLEADO), any())).thenReturn(Optional.of("EMPLEADO"));
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), any())).thenReturn(Optional.of("SOCIO"));
    }

    private String tokenAdmin() {
        return "Bearer " + jwtService.generarToken(ID_ADMIN, Rol.ADMINISTRADOR.name());
    }

    private String tokenEmpleado() {
        return "Bearer " + jwtService.generarToken(ID_EMPLEADO, Rol.EMPLEADO.name());
    }

    private String tokenSocio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, Rol.SOCIO.name());
    }

    @Test
    void soloAdmin_conEmpleado_403() throws Exception {
        mockMvc.perform(get("/api/descuentos").header("Authorization", tokenEmpleado()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_AUTORIZADO.name()));
    }

    @Test
    void soloAdmin_conSocio_403() throws Exception {
        mockMvc.perform(get("/api/descuentos").header("Authorization", tokenSocio()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_AUTORIZADO.name()));
    }

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/api/descuentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_AUTENTICADO.name()));
    }

    @Test
    void registrar_comoAdmin_201() throws Exception {
        DescuentoRequest req = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("15.00"), null, "Promo 10h", true);
        DescuentoResponse resp = new DescuentoResponse(1L, "PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("15.00"), null, "Promo 10h", true, 0L);

        when(descuentoService.registrar(any(DescuentoRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/descuentos")
                        .header("Authorization", tokenAdmin())
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/descuentos/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipo").value("PAQUETE_HORAS"))
                .andExpect(jsonPath("$.porcentaje").value(15.00));
    }

    @Test
    void registrar_invalido_400() throws Exception {
        mockMvc.perform(post("/api/descuentos")
                        .header("Authorization", tokenAdmin())
                        .contentType(JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION_FALLIDA.name()));
    }

    @Test
    void consultar_comoAdmin_200() throws Exception {
        DescuentoResponse resp = new DescuentoResponse(1L, "PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("15.00"), null, "Promo 10h", true, 3L);
        when(descuentoService.consultar(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/descuentos/1")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vecesAplicado").value(3));
    }

    @Test
    void consultar_inexistente_404() throws Exception {
        when(descuentoService.consultar(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.NO_ENCONTRADO, "Descuento no encontrado"));

        mockMvc.perform(get("/api/descuentos/99")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_ENCONTRADO.name()));
    }

    @Test
    void buscar_comoAdmin_200() throws Exception {
        DescuentoResponse resp = new DescuentoResponse(1L, "PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("15.00"), null, "Promo 10h", true, 0L);
        PageResponse<DescuentoResponse> pagina = new PageResponse<>(List.of(resp), 0, 20, 1L, 1);
        when(descuentoService.buscar(eq(TipoDescuento.PAQUETE_HORAS), eq(true), eq("promo"), eq(0), eq(20))).thenReturn(pagina);

        mockMvc.perform(get("/api/descuentos?tipo=PAQUETE_HORAS&activo=true&busqueda=promo&page=0&size=20")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void modificar_comoAdmin_200() throws Exception {
        DescuentoRequest req = new DescuentoRequest("LIGA20", TipoDescuento.LIGA_REGULAR, new BigDecimal("20.00"), null, "Liga act", true);
        DescuentoResponse resp = new DescuentoResponse(1L, "LIGA20", TipoDescuento.LIGA_REGULAR, new BigDecimal("20.00"), null, "Liga act", true, 2L);
        when(descuentoService.modificar(eq(1L), any(DescuentoRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/descuentos/1")
                        .header("Authorization", tokenAdmin())
                        .contentType(JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("LIGA_REGULAR"))
                .andExpect(jsonPath("$.porcentaje").value(20.00));
    }

    @Test
    void eliminar_comoAdmin_204() throws Exception {
        doNothing().when(descuentoService).eliminar(1L);

        mockMvc.perform(delete("/api/descuentos/1")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_enUso_409() throws Exception {
        doThrow(new ConflictoException(CodigoError.EN_USO, "Descuento en uso"))
                .when(descuentoService).eliminar(1L);

        mockMvc.perform(delete("/api/descuentos/1")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CodigoError.EN_USO.name()));
    }

    @Test
    void imprimir_comoAdmin_200Pdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 mock".getBytes();
        when(descuentoService.imprimir(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/descuentos/1/pdf")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"descuento-1.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }
}
