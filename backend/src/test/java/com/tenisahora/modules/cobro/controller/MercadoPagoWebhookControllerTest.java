package com.tenisahora.modules.cobro.controller;

import com.tenisahora.config.CorsConfig;
import com.tenisahora.config.SecurityConfig;
import com.tenisahora.middleware.JwtAuthenticationFilter;
import com.tenisahora.modules.cobro.service.AcreditacionAsincronica;
import com.tenisahora.modules.cobro.service.RegistroDeAvisosDescartados;
import com.tenisahora.modules.cobro.service.pago.VerificadorDeFirma;
import com.tenisahora.security.JwtService;
import com.tenisahora.security.LimitadorDeIntentos;
import com.tenisahora.security.VerificadorDeSesion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MercadoPagoWebhookController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, CorsConfig.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-con-largo-suficiente-para-hs384-0123456789",
        "app.jwt.expiration-minutes=30",
        "app.cors.allowed-origins=http://localhost:5173"
})
class MercadoPagoWebhookControllerTest {

    private static final String PAYLOAD = "{\"type\":\"payment\",\"data\":{\"id\":\"123456\"}}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificadorDeFirma verificadorDeFirma;
    @MockitoBean
    private AcreditacionAsincronica acreditacionAsincronica;
    @MockitoBean
    private RegistroDeAvisosDescartados registroDeAvisosDescartados;
    @MockitoBean
    private VerificadorDeSesion verificadorDeSesion;
    @MockitoBean
    private LimitadorDeIntentos limitadorDeIntentos;

    @Test
    void recibir_sinToken_igual_200_esPublico() throws Exception {
        when(verificadorDeFirma.esValida("123456", "pedido-1", "firma-valida")).thenReturn(true);

        mockMvc.perform(post("/webhooks/mercadopago").param("data.id", "123456").header("x-request-id", "pedido-1").header("x-signature", "firma-valida")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk());
    }

    @Test
    void recibir_firmaValida_disparaLaAcreditacionAsincronica() throws Exception {
        when(verificadorDeFirma.esValida("123456", "pedido-1", "firma-valida")).thenReturn(true);

        mockMvc.perform(post("/webhooks/mercadopago").param("data.id", "123456").header("x-request-id", "pedido-1").header("x-signature", "firma-valida")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk());

        verify(acreditacionAsincronica).procesarAvisoDePago("123456");
    }

    @Test
    void recibir_firmaInvalida_401_yElServicioNoSeToca() throws Exception {
        when(verificadorDeFirma.esValida("123456", "pedido-1", "firma-mala")).thenReturn(false);

        mockMvc.perform(post("/webhooks/mercadopago").param("data.id", "123456").header("x-request-id", "pedido-1").header("x-signature", "firma-mala")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(acreditacionAsincronica, never()).procesarAvisoDePago(any());
        verify(registroDeAvisosDescartados).registrar("123456", "pedido-1");
    }

    @Test
    void recibir_sinFirma_401() throws Exception {
        when(verificadorDeFirma.esValida("123456", null, null)).thenReturn(false);

        mockMvc.perform(post("/webhooks/mercadopago").param("data.id", "123456").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"no es json", "{}", "{\"type\":\"payment\"}", "{\"type\":\"payment\",\"data\":{}}", "{\"type\":\"payment\",\"data\":{\"id\":\"  \"}}", "{\"type\":\"payment\",\"data\":{\"id\":null}}"})
    void recibir_firmaValidaConPayloadSinPago_200_yNoProcesaNada(String payload) throws Exception {
        when(verificadorDeFirma.esValida("1", "pedido-1", "firma-valida")).thenReturn(true);

        mockMvc.perform(post("/webhooks/mercadopago").param("data.id", "1").header("x-request-id", "pedido-1").header("x-signature", "firma-valida")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        verify(acreditacionAsincronica, never()).procesarAvisoDePago(any());
        verify(registroDeAvisosDescartados, never()).registrar(any(), any());
    }

    @Test
    void recibir_sinFirmaNiDatos_igual401YQuedaRegistrado() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(registroDeAvisosDescartados).registrar(null, null);
    }

    @Test
    void recibir_otroTopicoQueNoEsPago_200_yNoProcesaNada() throws Exception {
        String otroTopico = "{\"type\":\"merchant_order\",\"data\":{\"id\":\"1\"}}";
        when(verificadorDeFirma.esValida("1", "pedido-1", "firma-valida")).thenReturn(true);

        mockMvc.perform(post("/webhooks/mercadopago").param("data.id", "1").header("x-request-id", "pedido-1").header("x-signature", "firma-valida")
                        .contentType(MediaType.APPLICATION_JSON).content(otroTopico))
                .andExpect(status().isOk());

        verify(acreditacionAsincronica, never()).procesarAvisoDePago(any());
    }
}
