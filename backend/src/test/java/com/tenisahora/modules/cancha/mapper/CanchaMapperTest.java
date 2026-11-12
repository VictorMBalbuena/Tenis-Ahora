package com.tenisahora.modules.cancha.mapper;

import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.entity.Cancha;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CanchaMapperTest {

    private final CanchaMapper mapper = new CanchaMapper();

    private static Cancha existente() {
        Cancha cancha = new Cancha();
        cancha.setId(1L);
        cancha.setIdentificador("Cancha 1");
        cancha.setTipoSuperficie("CEMENTO");
        cancha.setCapacidad(4);
        cancha.setPrecioPorHora(new BigDecimal("6000"));
        cancha.setActiva(true);
        return cancha;
    }

    @Test
    void aEntidad_copiaLosDatosYLaDejaActivaSinId() {
        Cancha cancha = mapper.aEntidad(new CanchaRequest("  Cancha 3 ", "PASTO", 2, new BigDecimal("7000")));

        assertThat(cancha.getId()).isNull();
        assertThat(cancha.getIdentificador()).isEqualTo("Cancha 3");
        assertThat(cancha.getTipoSuperficie()).isEqualTo("PASTO");
        assertThat(cancha.getCapacidad()).isEqualTo(2);
        assertThat(cancha.getPrecioPorHora()).isEqualByComparingTo("7000");
        assertThat(cancha.getActiva()).isTrue();
    }

    @Test
    void aRespuesta_incluyeElEstadoDeMantenimiento() {
        CanchaResponse respuesta = mapper.aRespuesta(existente(), true);

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.identificador()).isEqualTo("Cancha 1");
        assertThat(respuesta.enMantenimiento()).isTrue();
    }

    @Test
    void aplicar_conTodosLosCampos_losCambia() {
        Cancha cancha = existente();

        mapper.aplicar(new CanchaUpdateRequest("Central", "LADRILLO", 2, new BigDecimal("9000"), false), cancha);

        assertThat(cancha.getIdentificador()).isEqualTo("Central");
        assertThat(cancha.getTipoSuperficie()).isEqualTo("LADRILLO");
        assertThat(cancha.getCapacidad()).isEqualTo(2);
        assertThat(cancha.getPrecioPorHora()).isEqualByComparingTo("9000");
        assertThat(cancha.getActiva()).isFalse();
    }

    @Test
    void aplicar_conCamposNulos_noTocaNada() {
        Cancha cancha = existente();

        mapper.aplicar(new CanchaUpdateRequest(null, null, null, null, null), cancha);

        assertThat(cancha.getIdentificador()).isEqualTo("Cancha 1");
        assertThat(cancha.getTipoSuperficie()).isEqualTo("CEMENTO");
        assertThat(cancha.getCapacidad()).isEqualTo(4);
        assertThat(cancha.getPrecioPorHora()).isEqualByComparingTo("6000");
        assertThat(cancha.getActiva()).isTrue();
    }

    @Test
    void aplicar_nuncaCambiaElId() {
        Cancha cancha = existente();

        mapper.aplicar(new CanchaUpdateRequest("Otra", null, null, null, null), cancha);

        assertThat(cancha.getId()).isEqualTo(1L);
    }
}
