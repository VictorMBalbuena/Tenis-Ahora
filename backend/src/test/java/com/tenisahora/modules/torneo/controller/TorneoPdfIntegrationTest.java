package com.tenisahora.modules.torneo.controller;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.middleware.LimitePorIpFilter;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Partido;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.PartidoRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.modules.torneo.service.FixtureService;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FixtureController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TorneoPdfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FixtureService fixtureService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LimitePorIpFilter limitePorIpFilter;

    @Test
    @DisplayName("GET /api/torneos/{id}/resultados/pdf retorna PDF con magic bytes %PDF- y headers correctos")
    void descargarPdfResultados() throws Exception {
        byte[] pdfMock = "%PDF-1.4 mock content".getBytes(StandardCharsets.ISO_8859_1);
        when(fixtureService.generarPdfResultados(1L)).thenReturn(pdfMock);

        MvcResult result = mockMvc.perform(get("/api/torneos/1/resultados/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resultados-torneo-1.pdf\""))
                .andReturn();

        byte[] contenido = result.getResponse().getContentAsByteArray();
        assertThat(contenido).isNotEmpty();
        assertThat(new String(contenido, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("FixtureService genera ficha PDF válida con PdfService")
    void fixtureServiceGeneraPdfValido() {
        TorneoRepository torneoRepository = org.mockito.Mockito.mock(TorneoRepository.class);
        PartidoRepository partidoRepository = org.mockito.Mockito.mock(PartidoRepository.class);
        InscripcionRepository inscripcionRepository = org.mockito.Mockito.mock(InscripcionRepository.class);
        PdfService pdfService = new PdfService();

        FixtureService service = new FixtureService(torneoRepository, partidoRepository, inscripcionRepository, pdfService);

        Torneo torneo = new Torneo();
        torneo.setId(1L);
        torneo.setNombre("Torneo Clausura");
        torneo.setEstado(EstadoTorneo.FINALIZADO);

        Inscripcion i1 = new Inscripcion();
        Socio s1 = new Socio();
        s1.setNombre("Juan");
        s1.setApellido("Pérez");
        i1.setJugador1(s1);

        Inscripcion i2 = new Inscripcion();
        Socio s2 = new Socio();
        s2.setNombre("Pedro");
        s2.setApellido("Gómez");
        i2.setJugador1(s2);

        Partido p = new Partido();
        p.setId(10L);
        p.setTorneo(torneo);
        p.setRonda(1);
        p.setOrden(1);
        p.setParticipanteA(i1);
        p.setParticipanteB(i2);
        p.setSets("6-3 6-4");
        p.setResultado("Gana Juan Pérez");

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of(p));

        byte[] pdf = service.generarPdfResultados(1L);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
