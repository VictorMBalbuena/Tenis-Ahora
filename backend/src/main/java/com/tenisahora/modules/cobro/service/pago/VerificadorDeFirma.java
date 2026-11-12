package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.infrastructure.mercadopago.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Autentica el aviso del webhook (A07): no hay JWT, la autenticación es la firma de Mercado Pago. El header
 * {@code x-signature} trae {@code ts=...,v1=...}, y {@code v1} es el HMAC-SHA256 con la clave secreta de
 * {@code id:[data.id];request-id:[x-request-id];ts:[ts];}. Se compara en tiempo constante (A02).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificadorDeFirma {

    private static final String ALGORITMO = "HmacSHA256";

    private final MercadoPagoProperties propiedades;

    public boolean esValida(String dataId, String requestId, String firmaRecibida) {
        if (!propiedades.verificaFirmas() || firmaRecibida == null || firmaRecibida.isBlank()) {
            return false;
        }
        Map<String, String> partes = partesDe(firmaRecibida);
        String ts = partes.get("ts");
        String v1 = partes.get("v1");
        if (ts == null || v1 == null) {
            log.warn("Webhook de Mercado Pago con firma mal formada");
            return false;
        }
        try {
            return MessageDigest.isEqual(calcular(manifiesto(dataId, requestId, ts)), HexFormat.of().parseHex(v1));
        } catch (IllegalArgumentException ex) {
            log.warn("Webhook de Mercado Pago con firma mal formada");
            return false;
        }
    }

    private Map<String, String> partesDe(String firma) {
        Map<String, String> partes = new HashMap<>();
        for (String parte : firma.split(",")) {
            String[] claveValor = parte.split("=", 2);
            if (claveValor.length == 2) {
                partes.put(claveValor[0].strip(), claveValor[1].strip());
            }
        }
        return partes;
    }

    /** Lo que Mercado Pago no manda se omite del texto firmado; un id alfanumérico se firma en minúsculas. */
    private String manifiesto(String dataId, String requestId, String ts) {
        StringBuilder texto = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            texto.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (requestId != null && !requestId.isBlank()) {
            texto.append("request-id:").append(requestId).append(';');
        }
        return texto.append("ts:").append(ts).append(';').toString();
    }

    private byte[] calcular(String texto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(propiedades.webhookSecret().getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", ex);
        }
    }
}
