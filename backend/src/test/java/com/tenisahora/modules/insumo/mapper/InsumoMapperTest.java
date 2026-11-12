package com.tenisahora.modules.insumo.mapper;

import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.entity.Insumo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsumoMapperTest {

    private final InsumoMapper mapper = new InsumoMapper();
    private static final CategoriaInsumo RED = categoria(1L, "RED");
    private static final CategoriaInsumo PELOTA = categoria(2L, "PELOTA");

    private static CategoriaInsumo categoria(Long id, String nombre) {
        CategoriaInsumo categoria = new CategoriaInsumo(nombre);
        categoria.setId(id);
        return categoria;
    }

    private static Insumo insumo(int cantidad, int minima) {
        Insumo insumo = new Insumo();
        insumo.setId(1L);
        insumo.setNombre("Redes");
        insumo.setCategoria(RED);
        insumo.setCantidadDisponible(cantidad);
        insumo.setCantidadMinima(minima);
        insumo.setActivo(true);
        return insumo;
    }

    @Test
    void aEntidad_copiaLosDatosYLaDejaActivaSinId() {
        Insumo insumo = mapper.aEntidad(new InsumoRequest("  Redes ", RED.getId(), 7, 8), RED);

        assertThat(insumo.getId()).isNull();
        assertThat(insumo.getNombre()).isEqualTo("Redes");
        assertThat(insumo.getCategoria()).isEqualTo(RED);
        assertThat(insumo.getCantidadDisponible()).isEqualTo(7);
        assertThat(insumo.getCantidadMinima()).isEqualTo(8);
        assertThat(insumo.getActivo()).isTrue();
    }

    @Test
    void estadoDe_sinNada_esSinStock() {
        assertThat(mapper.estadoDe(insumo(0, 5))).isEqualTo(EstadoStock.SIN_STOCK);
        assertThat(mapper.estadoDe(insumo(0, 0))).isEqualTo(EstadoStock.SIN_STOCK);
    }

    @Test
    void estadoDe_hastaElMinimo_esStockBajo() {
        assertThat(mapper.estadoDe(insumo(1, 5))).isEqualTo(EstadoStock.STOCK_BAJO);
        assertThat(mapper.estadoDe(insumo(5, 5))).isEqualTo(EstadoStock.STOCK_BAJO);
    }

    @Test
    void estadoDe_porEncimaDelMinimo_esDisponible() {
        assertThat(mapper.estadoDe(insumo(6, 5))).isEqualTo(EstadoStock.DISPONIBLE);
        assertThat(mapper.estadoDe(insumo(3, 0))).isEqualTo(EstadoStock.DISPONIBLE);
    }

    @Test
    void aRespuesta_incluyeLasUnidadesEnUsoYElEstado() {
        InsumoResponse respuesta = mapper.aRespuesta(insumo(7, 8), 6);

        assertThat(respuesta.enUso()).isEqualTo(6);
        assertThat(respuesta.estado()).isEqualTo(EstadoStock.STOCK_BAJO);
        assertThat(respuesta.cantidadDisponible()).isEqualTo(7);
        assertThat(respuesta.stockTotal()).isEqualTo(13);
        assertThat(respuesta.categoria().id()).isEqualTo(RED.getId());
    }

    @Test
    void aplicar_cambiaLoQueVieneYNoToca_laCantidadNiElId() {
        Insumo insumo = insumo(7, 8);

        mapper.aplicar(new InsumoUpdateRequest("Redes de torneo", PELOTA.getId(), 2, 99), insumo, PELOTA);

        assertThat(insumo.getNombre()).isEqualTo("Redes de torneo");
        assertThat(insumo.getCategoria()).isEqualTo(PELOTA);
        assertThat(insumo.getCantidadMinima()).isEqualTo(2);
        assertThat(insumo.getCantidadDisponible()).isEqualTo(7);
        assertThat(insumo.getId()).isEqualTo(1L);
    }

    @Test
    void aplicar_conCamposNulos_noTocaNada() {
        Insumo insumo = insumo(7, 8);

        mapper.aplicar(new InsumoUpdateRequest(null, null, null, null), insumo, null);

        assertThat(insumo.getNombre()).isEqualTo("Redes");
        assertThat(insumo.getCategoria()).isEqualTo(RED);
        assertThat(insumo.getCantidadMinima()).isEqualTo(8);
    }
}
