package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.RELOJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasDeReservaTest {

    private final ReglasDeReserva reglas = new ReglasDeReserva(PROPIEDADES, RELOJ);

    private static LocalTime hora(int hora) {
        return LocalTime.of(hora, 0);
    }

    @Test
    void validarTurno_conUnTurnoValido_devuelveLaFranjaQueOcupa() {
        FranjaHoraria franja = reglas.validarTurno(HOY.plusDays(3), hora(18), 2);

        assertThat(franja).isEqualTo(new FranjaHoraria(hora(18), hora(20)));
    }

    @Test
    void validarTurno_exactamenteA30Dias_sePermite_RD06() {
        assertThat(reglas.validarTurno(HOY.plusDays(30), hora(18), 1)).isNotNull();
    }

    @Test
    void validarTurno_a31Dias_422FueraDePlazo_A1() {
        assertThatThrownBy(() -> reglas.validarTurno(HOY.plusDays(31), hora(18), 1))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("30 días")
                .extracting("codigo").isEqualTo("RESERVA_FUERA_DE_PLAZO");
    }

    @Test
    void validarTurno_conUnDiaPasado_422() {
        assertThatThrownBy(() -> reglas.validarTurno(HOY.minusDays(1), hora(18), 1))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("RESERVA_FECHA_PASADA");
    }

    @Test
    void validarTurno_hoyAUnaHoraQueYaPaso_422_yHoyMasTarde_sePermite() {
        assertThatThrownBy(() -> reglas.validarTurno(HOY, hora(9), 1)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> reglas.validarTurno(HOY, hora(12), 1)).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(reglas.validarTurno(HOY, hora(13), 1)).isNotNull();
    }

    @Test
    void validarTurno_conDuracionDeCeroOMasDeDosHoras_400_RD17() {
        assertThatThrownBy(() -> reglas.validarTurno(HOY.plusDays(1), hora(18), 0))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RESERVA_DURACION_INVALIDA");
        assertThatThrownBy(() -> reglas.validarTurno(HOY.plusDays(1), hora(18), 3))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("1 a 2 horas");
    }

    @Test
    void validarTurno_conUnaHoraQueNoEsEnPunto_400() {
        assertThatThrownBy(() -> reglas.validarTurno(HOY.plusDays(1), LocalTime.of(18, 30), 1))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("RESERVA_DURACION_INVALIDA");
    }

    @Test
    void validarTurno_queCruzariaLaMedianoche_400() {
        assertThatThrownBy(() -> reglas.validarTurno(HOY.plusDays(1), hora(23), 2))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("medianoche");
    }

    @Test
    void validarAcompaniantes_unoOTres_sePermite_RD07() {
        reglas.validarAcompaniantes(1);
        reglas.validarAcompaniantes(3);
    }

    @Test
    void validarAcompaniantes_cualquierOtraCantidad_400() {
        for (int cantidad : new int[]{0, 2, 4, 10}) {
            assertThatThrownBy(() -> reglas.validarAcompaniantes(cantidad))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("2 o 4 jugadores")
                    .extracting("codigo").isEqualTo("RESERVA_JUGADORES_INVALIDOS");
        }
    }

    @Test
    void esReservable_desdeHoyHastaHoyMasLaAntelacion() {
        assertThat(reglas.esReservable(HOY)).isTrue();
        assertThat(reglas.esReservable(HOY.plusDays(30))).isTrue();
        assertThat(reglas.esReservable(HOY.plusDays(31))).isFalse();
        assertThat(reglas.esReservable(HOY.minusDays(1))).isFalse();
        assertThat(reglas.esReservable(LocalDate.of(2027, 1, 1))).isFalse();
    }
}
