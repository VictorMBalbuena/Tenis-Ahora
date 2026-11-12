package com.tenisahora.modules.torneo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.middleware.LimitePorIpFilter;
import com.tenisahora.modules.torneo.dto.InscribirTorneoRequest;
import com.tenisahora.modules.torneo.dto.InscripcionResponse;
import com.tenisahora.modules.torneo.service.InscripcionService;
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

@WebMvcTest(InscripcionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InscripcionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InscripcionService inscripcionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LimitePorIpFilter limitePorIpFilter;

    @Test
    @DisplayName("POST /api/torneos/{id}/inscripciones registra una inscripción y retorna 201")
    void inscribirTorneo() throws Exception {
        InscribirTorneoRequest request = new InscribirTorneoRequest(10L, null, "Los Invencibles");
        InscripcionResponse response = new InscripcionResponse(
                1L, 1L, 10L, "Carlos Alcaraz", null, null, 1, "Los Invencibles", LocalDate.now(), null, null, null, null);

        when(inscripcionService.inscribir(eq(1L), any(InscribirTorneoRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/torneos/1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.jugador1Nombre").value("Carlos Alcaraz"))
                .andExpect(jsonPath("$.equipo").value(1));
    }

    @Test
    @DisplayName("GET /api/torneos/{id}/inscripciones lista las inscripciones y retorna 200")
    void listarInscripciones() throws Exception {
        InscripcionResponse response = new InscripcionResponse(
                1L, 1L, 10L, "Carlos Alcaraz", null, null, 1, "Los Invencibles", LocalDate.now(), null, null, null, null);

        when(inscripcionService.listarInscripciones(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/torneos/1/inscripciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].jugador1Nombre").value("Carlos Alcaraz"));
    }
}
