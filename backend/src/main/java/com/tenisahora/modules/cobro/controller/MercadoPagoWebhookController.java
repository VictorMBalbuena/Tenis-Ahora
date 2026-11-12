package com.tenisahora.modules.cobro.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenisahora.modules.cobro.service.AcreditacionAsincronica;
import com.tenisahora.modules.cobro.service.RegistroDeAvisosDescartados;
import com.tenisahora.modules.cobro.service.pago.VerificadorDeFirma;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Aviso de pagos de Mercado Pago (CU78). Va fuera de {@code /api}: no es parte de la API del front, así que queda
 * fuera de la config de CORS (registrada solo en {@code /api/**}) y del límite por IP (allow-list por ruta exacta).
 * No lleva JWT: la firma HMAC es su autenticación (A07).
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/mercadopago")
@RequiredArgsConstructor
@Tag(name = "Webhook de Mercado Pago", description = "Aviso asíncrono de pagos (CU78). Endpoint público, fuera de /api.")
public class MercadoPagoWebhookController {

    private final VerificadorDeFirma verificadorDeFirma;
    private final AcreditacionAsincronica acreditacionAsincronica;
    private final RegistroDeAvisosDescartados registroDeAvisosDescartados;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Recibe el aviso de un pago",
            description = "Valida la firma en el momento (401 si es inválida, y no procesa nada). Si es válida, responde 200 enseguida "
                    + "y la acreditación se procesa en otro hilo: Mercado Pago no tiene que esperar a que termine.")
    @ApiResponse(responseCode = "401", description = "Firma inválida o ausente: el aviso se descarta sin procesar y queda registrado")
    public ResponseEntity<Void> recibir(@RequestBody String payload,
                                        @RequestParam(value = "data.id", required = false) String dataId,
                                        @RequestHeader(value = "x-request-id", required = false) String requestId,
                                        @RequestHeader(value = "x-signature", required = false) String firma) {
        if (!verificadorDeFirma.esValida(dataId, requestId, firma)) {
            log.warn("Webhook de Mercado Pago con firma inválida: aviso descartado sin procesar");
            registroDeAvisosDescartados.registrar(dataId, requestId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        idDePago(payload).ifPresent(acreditacionAsincronica::procesarAvisoDePago);
        return ResponseEntity.ok().build();
    }

    /** Solo interesan los avisos de tipo "payment": otros tópicos (merchant_order, etc.) se ignoran. */
    private Optional<String> idDePago(String payload) {
        try {
            JsonNode raiz = objectMapper.readTree(payload);
            if (!"payment".equals(raiz.path("type").asText())) {
                return Optional.empty();
            }
            String id = raiz.path("data").path("id").asText(null);
            return Optional.ofNullable(id).filter(valor -> !valor.isBlank());
        } catch (JsonProcessingException ex) {
            log.warn("Webhook de Mercado Pago con un payload que no es JSON válido");
            return Optional.empty();
        }
    }
}
