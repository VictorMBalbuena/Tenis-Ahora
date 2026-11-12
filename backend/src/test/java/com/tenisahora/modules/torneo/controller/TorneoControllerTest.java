package com.tenisahora.modules.torneo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.middleware.LimitePorIpFilter;
import com.tenisahora.modules.torneo.dto.CrearTorneoRequest;
import com.tenisahora.modules.torneo.dto.TorneoResponse;
import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import com.tenisahora.modules.torneo.service.TorneoService;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TorneoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TorneoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TorneoService torneoService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LimitePorIpFilter limitePorIpFilter;

    @Test
    @DisplayName("POST /api/torneos crea un nuevo torneo y retorna 201")
    void crearTorneo() throws Exception {
        CrearTorneoRequest request = new CrearTorneoRequest(
                "Torneo Verano",
                1L,
                CategoriaTorneo.MASCULINO,
                Modalidad.SINGLE,
                "LADRILLO",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                BigDecimal.valueOf(10000), null);

        TorneoResponse response = new TorneoResponse(
                1L,
                "Torneo Verano",
                TipoCompetencia.TORNEO,
                1L,
                "Cancha 1",
                CategoriaTorneo.MASCULINO,
                Modalidad.SINGLE,
                "LADRILLO",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                EstadoTorneo.PLANIFICADO,
                BigDecimal.valueOf(10000), null, null);

        when(torneoService.crearTorneo(any(CrearTorneoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/torneos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Torneo Verano"))
                .andExpect(jsonPath("$.estado").value("PLANIFICADO"));
    }

    @Test
    @DisplayName("GET /api/torneos/{id} retorna torneo existente o 404 si no existe")
    void obtenerTorneoPorId() throws Exception {
        TorneoResponse response = new TorneoResponse(
                1L,
                "Torneo Verano",
                TipoCompetencia.TORNEO,
                1L,
                "Cancha 1",
                CategoriaTorneo.MASCULINO,
                Modalidad.SINGLE,
                "LADRILLO",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                EstadoTorneo.PLANIFICADO,
                BigDecimal.valueOf(10000), null, null);

        when(torneoService.obtenerTorneo(1L)).thenReturn(response);
        when(torneoService.obtenerTorneo(99L)).thenThrow(new RecursoNoEncontradoException(CodigoError.TORNEO_NO_ENCONTRADO, "Torneo no encontrado"));

        mockMvc.perform(get("/api/torneos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Torneo Verano"));

        mockMvc.perform(get("/api/torneos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TORNEO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("DELETE /api/torneos/{id} retorna 204")
    void eliminarTorneo() throws Exception {
        mockMvc.perform(delete("/api/torneos/1"))
                .andExpect(status().isNoContent());

        verify(torneoService).eliminarTorneo(1L);
    }
}
