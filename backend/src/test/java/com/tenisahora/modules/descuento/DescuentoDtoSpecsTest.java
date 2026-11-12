package com.tenisahora.modules.descuento;

import com.tenisahora.modules.descuento.dto.DescuentoRequest;
import com.tenisahora.modules.descuento.dto.DescuentoResponse;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.repository.DescuentoSpecs;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class DescuentoDtoSpecsTest {
    @Test
    void dtosInstancianCorrectamente() {
        DescuentoRequest req = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("10.00"), null, "10 horas", true);
        assertThat(req.tipo()).isEqualTo(TipoDescuento.PAQUETE_HORAS);
        assertThat(req.porcentaje()).isEqualByComparingTo("10.00");

        DescuentoResponse resp = new DescuentoResponse(1L, "PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("10.00"), null, "10 horas", true, 5L);
        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.vecesAplicado()).isEqualTo(5L);
    }

    @Test
    void specsNoSonNulas() {
        assertThat(DescuentoSpecs.porTipo(TipoDescuento.PAQUETE_HORAS)).isNotNull();
        assertThat(DescuentoSpecs.activo(true)).isNotNull();
        assertThat(DescuentoSpecs.condicionesContiene("promo")).isNotNull();
    }
}
