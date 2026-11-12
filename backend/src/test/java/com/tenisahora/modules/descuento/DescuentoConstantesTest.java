package com.tenisahora.modules.descuento;

import com.tenisahora.shared.exception.CodigoError;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DescuentoConstantesTest {
    @Test
    void codigosDeErrorExisten() {
        assertThat(CodigoError.valueOf("DESCUENTO_DUPLICADO")).isNotNull();
        assertThat(CodigoError.valueOf("BENEFICIO_INVALIDO")).isNotNull();
        assertThat(CodigoError.valueOf("NO_ENCONTRADO")).isNotNull();
        assertThat(CodigoError.valueOf("EN_USO")).isNotNull();
    }
}
