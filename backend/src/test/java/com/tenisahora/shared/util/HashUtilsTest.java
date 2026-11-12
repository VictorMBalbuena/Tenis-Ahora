package com.tenisahora.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    void sha256Hex_coincideConElValorConocido() {
        assertThat(HashUtils.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sha256Hex_siempreDaElMismoValorParaLaMismaEntrada() {
        assertThat(HashUtils.sha256Hex("refresh-token")).isEqualTo(HashUtils.sha256Hex("refresh-token"));
    }

    @Test
    void sha256Hex_entradasDistintasDanValoresDistintos() {
        assertThat(HashUtils.sha256Hex("uno")).isNotEqualTo(HashUtils.sha256Hex("dos"));
    }

    @Test
    void sha256Hex_tiene64CaracteresHexadecimales() {
        assertThat(HashUtils.sha256Hex("cualquier cosa")).hasSize(64).matches("[0-9a-f]+");
    }
}
