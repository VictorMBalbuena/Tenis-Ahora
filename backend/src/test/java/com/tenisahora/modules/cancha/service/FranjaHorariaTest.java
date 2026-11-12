package com.tenisahora.modules.cancha.service;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FranjaHorariaTest {

    private static FranjaHoraria franja(int desde, int hasta) {
        return new FranjaHoraria(LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    @Test
    void validada_conHorasEnPunto_armaLaFranja() {
        FranjaHoraria franja = FranjaHoraria.validada(LocalTime.of(8, 0), LocalTime.of(12, 0), CodigoError.HORARIO_INVALIDO);

        assertThat(franja).isEqualTo(franja(8, 12));
    }

    @Test
    void validada_conMinutos_rechazaConElCodigoIndicado() {
        assertThatThrownBy(() -> FranjaHoraria.validada(LocalTime.of(8, 30), LocalTime.of(12, 0), CodigoError.BLOQUEO_INVALIDO))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("BLOQUEO_INVALIDO");
    }

    @Test
    void validada_conSegundos_rechaza() {
        assertThatThrownBy(() -> FranjaHoraria.validada(LocalTime.of(8, 0), LocalTime.of(12, 0, 15), CodigoError.HORARIO_INVALIDO))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void validada_conInicioIgualOPosteriorAlFin_rechaza() {
        assertThatThrownBy(() -> FranjaHoraria.validada(LocalTime.of(12, 0), LocalTime.of(12, 0), CodigoError.HORARIO_INVALIDO))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> FranjaHoraria.validada(LocalTime.of(14, 0), LocalTime.of(9, 0), CodigoError.HORARIO_INVALIDO))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void seSuperponeCon_franjasQueSePisan_true() {
        assertThat(franja(8, 12).seSuperponeCon(franja(10, 14))).isTrue();
        assertThat(franja(10, 14).seSuperponeCon(franja(8, 12))).isTrue();
        assertThat(franja(8, 18).seSuperponeCon(franja(10, 12))).isTrue();
    }

    @Test
    void seSuperponeCon_franjasSeguidas_false() {
        assertThat(franja(8, 10).seSuperponeCon(franja(10, 12))).isFalse();
        assertThat(franja(10, 12).seSuperponeCon(franja(8, 10))).isFalse();
    }

    @Test
    void seSuperponeCon_franjasSeparadas_false() {
        assertThat(franja(8, 10).seSuperponeCon(franja(14, 16))).isFalse();
    }
}
