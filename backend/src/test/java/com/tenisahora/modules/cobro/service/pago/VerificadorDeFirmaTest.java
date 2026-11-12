package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.infrastructure.mercadopago.MercadoPagoProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class VerificadorDeFirmaTest {

    private static final MercadoPagoProperties PROPIEDADES =
            new MercadoPagoProperties("token", "el-secreto", "https://api.mercadopago.com", 10, false);
    private static final String TS = "1704908010";

    private final VerificadorDeFirma verificador = new VerificadorDeFirma(PROPIEDADES);

    private String firmar(String secreto, String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "ts=" + TS + ",v1=" + HexFormat.of().formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String manifiesto(String id, String requestId) {
        return "id:" + id + ";request-id:" + requestId + ";ts:" + TS + ";";
    }

    @Test
    void esValida_firmaCorrecta_true() {
        assertThat(verificador.esValida("123", "pedido-1", firmar("el-secreto", manifiesto("123", "pedido-1")))).isTrue();
    }

    @Test
    void esValida_idAlfanumerico_seFirmaEnMinusculas() {
        assertThat(verificador.esValida("ORD01ABC", "pedido-1", firmar("el-secreto", manifiesto("ord01abc", "pedido-1")))).isTrue();
    }

    @Test
    void esValida_sinRequestId_loOmiteDelTextoFirmado() {
        assertThat(verificador.esValida("123", null, firmar("el-secreto", "id:123;ts:" + TS + ";"))).isTrue();
    }

    @Test
    void esValida_firmaConOtroSecreto_false() {
        assertThat(verificador.esValida("123", "pedido-1", firmar("otro-secreto", manifiesto("123", "pedido-1")))).isFalse();
    }

    @Test
    void esValida_otroPago_false() {
        assertThat(verificador.esValida("124", "pedido-1", firmar("el-secreto", manifiesto("123", "pedido-1")))).isFalse();
    }

    @Test
    void esValida_firmaAusenteOVacia_false() {
        assertThat(verificador.esValida("123", "pedido-1", null)).isFalse();
        assertThat(verificador.esValida("123", "pedido-1", "")).isFalse();
        assertThat(verificador.esValida("123", "pedido-1", "   ")).isFalse();
    }

    @Test
    void esValida_firmaMalFormada_false() {
        assertThat(verificador.esValida("123", "pedido-1", "no-es-una-firma")).isFalse();
        assertThat(verificador.esValida("123", "pedido-1", "ts=1,v1=no-es-hexadecimal")).isFalse();
    }

    @Test
    void esValida_sinSecretoConfigurado_siempreFalse() {
        MercadoPagoProperties sinSecreto = new MercadoPagoProperties("token", "", "https://api.mercadopago.com", 10, false);
        VerificadorDeFirma verificadorSinSecreto = new VerificadorDeFirma(sinSecreto);

        assertThat(verificadorSinSecreto.esValida("123", "pedido-1", firmar("cualquiera", manifiesto("123", "pedido-1")))).isFalse();
    }
}
