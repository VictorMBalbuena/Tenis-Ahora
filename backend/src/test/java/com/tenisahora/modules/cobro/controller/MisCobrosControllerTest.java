package com.tenisahora.modules.cobro.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.cobro.service.AnulacionDeCobros;
import com.tenisahora.modules.cobro.service.CobroService;
import com.tenisahora.modules.cobro.service.QrDeCobro;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.GlobalExceptionHandler;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MisCobrosController.class, CobroController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class MisCobrosControllerTest {

    private static final long ID_SOCIO = 3L;
    private static final String URL = "/api/cobros/mios";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CobroService cobroService;
    @MockitoBean
    private QrDeCobro qrDeCobro;
    @MockitoBean
    private AnulacionDeCobros anulacionDeCobros;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @BeforeEach
    void setUp() {
        for (Rol rol : Rol.values()) {
            when(verificadorDeSesion.rolVigente(eq(10L + rol.ordinal()), anyString())).thenReturn(Optional.of(rol.name()));
        }
        when(verificadorDeSesion.rolVigente(eq(ID_SOCIO), anyString())).thenReturn(Optional.of("SOCIO"));
        when(cobroService.delSocio(anyLong(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
    }

    private String socio() {
        return "Bearer " + jwtService.generarToken(ID_SOCIO, Rol.SOCIO.name());
    }

    @Test
    void mios_sinToken_401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, mode = EnumSource.Mode.EXCLUDE, names = "SOCIO")
    void mios_otroRol_403(Rol rol) throws Exception {
        String token = "Bearer " + jwtService.generarToken(10L + rol.ordinal(), rol.name());

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isForbidden());

        verify(cobroService, never()).delSocio(anyLong(), anyInt(), anyInt());
    }

    @Test
    void mios_usaElSocioDeLaSesionSinCache_yNoLoConfundeConUnId() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", socio()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(cobroService).delSocio(ID_SOCIO, 0, 20);
        verify(cobroService, never()).consultar(any(), any());
    }

    @Test
    void mios_unParametroConOtroSocio_seIgnora() throws Exception {
        mockMvc.perform(get(URL + "?socioId=99").header("Authorization", socio())).andExpect(status().isOk());

        verify(cobroService).delSocio(ID_SOCIO, 0, 20);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=abc", "size=0", "size=51", "size=abc"})
    void mios_paginacionInvalida_400(String filtro) throws Exception {
        mockMvc.perform(get(URL + "?" + filtro).header("Authorization", socio())).andExpect(status().isBadRequest());

        verify(cobroService, never()).delSocio(anyLong(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0", "page=5", "size=1", "size=50"})
    void mios_paginacionEnElLimite_200(String filtro) throws Exception {
        mockMvc.perform(get(URL + "?" + filtro).header("Authorization", socio())).andExpect(status().isOk());
    }
}
