package com.tenisahora.modules.torneo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.middleware.LimitePorIpFilter;
import com.tenisahora.modules.torneo.dto.CargarResultadoRequest;
import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.service.FixtureService;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FixtureController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FixtureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FixtureService fixtureService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LimitePorIpFilter limitePorIpFilter;

    @Test
    @DisplayName("POST /api/torneos/{id}/fixture genera y retorna la lista de partidos")
    void generarFixture() throws Exception {
        PartidoResponse partido = new PartidoResponse(
                1L, 1L, 1, 1, 10L, "Jugador 1", 20L, "Jugador 2", null, null, LocalDate.now()
        );
        when(fixtureService.generarFixture(1L)).thenReturn(List.of(partido));

        mockMvc.perform(post("/api/torneos/1/fixture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].ronda").value(1));
    }

    @Test
    @DisplayName("GET /api/torneos/{id}/partidos retorna el listado de partidos")
    void listarPartidos() throws Exception {
        PartidoResponse partido = new PartidoResponse(
                1L, 1L, 1, 1, 10L, "Jugador 1", 20L, "Jugador 2", null, null, LocalDate.now()
        );
        when(fixtureService.listarPartidos(1L)).thenReturn(List.of(partido));

        mockMvc.perform(get("/api/torneos/1/partidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("POST /api/torneos/{id}/partidos/{partidoId}/resultado carga el resultado")
    void cargarResultado() throws Exception {
        CargarResultadoRequest req = new CargarResultadoRequest("6-4 6-2");
        PartidoResponse response = new PartidoResponse(
                10L, 1L, 1, 1, 10L, "Jugador 1", 20L, "Jugador 2", "6-4 6-2", "Gana Jugador 1", LocalDate.now()
        );

        when(fixtureService.cargarResultado(eq(1L), eq(10L), any(CargarResultadoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/torneos/1/partidos/10/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.sets").value("6-4 6-2"))
                .andExpect(jsonPath("$.resultado").value("Gana Jugador 1"));
    }
}
