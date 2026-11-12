package com.tenisahora.modules.actividad.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoActividadTest {

    @Test
    void ocupaCancha_soloProgramadaYEnCurso() {
        assertThat(EstadoActividad.PROGRAMADA.ocupaCancha()).isTrue();
        assertThat(EstadoActividad.EN_CURSO.ocupaCancha()).isTrue();
        assertThat(EstadoActividad.FINALIZADA.ocupaCancha()).isFalse();
        assertThat(EstadoActividad.CANCELADA.ocupaCancha()).isFalse();
    }

    @Test
    void admiteInscripcion_soloProgramada() {
        assertThat(EstadoActividad.PROGRAMADA.admiteInscripcion()).isTrue();
        assertThat(EstadoActividad.EN_CURSO.admiteInscripcion()).isFalse();
        assertThat(EstadoActividad.FINALIZADA.admiteInscripcion()).isFalse();
        assertThat(EstadoActividad.CANCELADA.admiteInscripcion()).isFalse();
    }

    @Test
    void puedeModificarse_soloProgramada() {
        assertThat(EstadoActividad.PROGRAMADA.puedeModificarse()).isTrue();
        assertThat(EstadoActividad.EN_CURSO.puedeModificarse()).isFalse();
        assertThat(EstadoActividad.FINALIZADA.puedeModificarse()).isFalse();
        assertThat(EstadoActividad.CANCELADA.puedeModificarse()).isFalse();
    }

    @Test
    void queOcupanCancha_devuelveProgramadaYEnCurso() {
        assertThat(EstadoActividad.queOcupanCancha()).containsExactly(EstadoActividad.PROGRAMADA, EstadoActividad.EN_CURSO);
    }
}
