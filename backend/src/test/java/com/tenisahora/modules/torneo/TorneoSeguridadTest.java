package com.tenisahora.modules.torneo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.torneo.controller.FixtureController;
import com.tenisahora.modules.torneo.controller.InscripcionController;
import com.tenisahora.modules.torneo.controller.TorneoController;
import com.tenisahora.modules.torneo.dto.CargarResultadoRequest;
import com.tenisahora.modules.torneo.dto.CrearTorneoRequest;
import com.tenisahora.modules.torneo.dto.InscribirTorneoRequest;
import com.tenisahora.modules.torneo.dto.InscripcionResponse;
import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.dto.TorneoResponse;
import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import com.tenisahora.modules.torneo.service.FixtureService;
import com.tenisahora.modules.torneo.service.InscripcionService;
import com.tenisahora.modules.torneo.service.TorneoService;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { TorneoController.class, InscripcionController.class, FixtureController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class,
                GlobalExceptionHandler.class })
@TestPropertySource(properties = {
                "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
                "app.jwt.expiration-minutes=30",
                "app.cors.allowed-origins=http://localhost:5173"
})
class TorneoSeguridadTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JwtService jwtService;

        @MockitoBean
        private TorneoService torneoService;

        @MockitoBean
        private InscripcionService inscripcionService;

        @MockitoBean
        private FixtureService fixtureService;

        @MockitoBean
        private VerificadorDeSesion verificadorDeSesion;

        @MockitoBean
        private LimitadorDeIntentos limitadorDeIntentos;

        @BeforeEach
        void setUp() {
                for (Rol rol : Rol.values()) {
                        when(verificadorDeSesion.rolVigente(eq((long) rol.ordinal() + 1), any()))
                                        .thenReturn(Optional.of(rol.name()));
                }
        }

        private String bearer(Rol rol) {
                Socio s = new Socio();
                s.setId((long) rol.ordinal() + 1);
                return "Bearer " + jwtService.generarToken(s.getId(), rol.name());
        }

        @Test
        @DisplayName("Usuario anónimo recibe 401 en endpoints protegidos")
        void usuarioAnonimoRecibe401() throws Exception {
                mockMvc.perform(get("/api/torneos"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

                mockMvc.perform(post("/api/torneos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }

        @Test
        @DisplayName("SOCIO recibe 403 al intentar crear torneo o cargar resultado")
        void socioRecibe403EnMutacionesDeTorneoYFixture() throws Exception {
                CrearTorneoRequest crearReq = new CrearTorneoRequest(
                                "Torneo", 1L, CategoriaTorneo.MASCULINO, Modalidad.SINGLE,
                                "LADRILLO", LocalDate.now(), LocalDate.now().plusDays(5), BigDecimal.TEN, null);

                mockMvc.perform(post("/api/torneos")
                                .header("Authorization", bearer(Rol.SOCIO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(crearReq)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));

                CargarResultadoRequest resReq = new CargarResultadoRequest("6-4 6-2");
                mockMvc.perform(post("/api/torneos/1/partidos/10/resultado")
                                .header("Authorization", bearer(Rol.SOCIO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resReq)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.codigo").value("NO_AUTORIZADO"));
        }

        @Test
        @DisplayName("SOCIO puede consultar torneos e inscribirse exitosamente")
        void socioPuedeConsultarEInscribirse() throws Exception {
                when(torneoService.listarTorneos()).thenReturn(List.of());
                InscribirTorneoRequest insReq = new InscribirTorneoRequest(1L, null, null);
                InscripcionResponse insResp = new InscripcionResponse(
                                1L, 1L, 1L, "Juan", null, null, 1, null, LocalDate.now(), null, null, null, null);
                when(inscripcionService.inscribir(eq(1L), any(InscribirTorneoRequest.class), any()))
                                .thenReturn(insResp);

                mockMvc.perform(get("/api/torneos")
                                .header("Authorization", bearer(Rol.SOCIO)))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/torneos/1/inscripciones")
                                .header("Authorization", bearer(Rol.SOCIO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(insReq)))
                                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("EMPLEADO y ADMINISTRADOR pueden crear torneo y generar fixture")
        void personalPuedeCrearTorneoYGenerarFixture() throws Exception {
                CrearTorneoRequest crearReq = new CrearTorneoRequest(
                                "Torneo", 1L, CategoriaTorneo.MASCULINO, Modalidad.SINGLE,
                                "LADRILLO", LocalDate.now(), LocalDate.now().plusDays(5), BigDecimal.TEN, null);
                TorneoResponse torneoResp = new TorneoResponse(
                                1L, "Torneo", TipoCompetencia.TORNEO, 1L, "Cancha 1", CategoriaTorneo.MASCULINO, Modalidad.SINGLE,
                                "LADRILLO", LocalDate.now(), LocalDate.now().plusDays(5),
                                EstadoTorneo.PLANIFICADO, BigDecimal.TEN, null, null);

                when(torneoService.crearTorneo(any(CrearTorneoRequest.class))).thenReturn(torneoResp);
                when(fixtureService.generarFixture(1L)).thenReturn(List.of());

                // Probar con EMPLEADO
                mockMvc.perform(post("/api/torneos")
                                .header("Authorization", bearer(Rol.EMPLEADO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(crearReq)))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/torneos/1/fixture")
                                .header("Authorization", bearer(Rol.EMPLEADO)))
                                .andExpect(status().isOk());

                // Probar con ADMINISTRADOR
                mockMvc.perform(post("/api/torneos")
                                .header("Authorization", bearer(Rol.ADMINISTRADOR))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(crearReq)))
                                .andExpect(status().isCreated());
        }
}
