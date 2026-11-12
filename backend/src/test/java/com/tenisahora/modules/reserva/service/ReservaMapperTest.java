package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.modules.reserva.mapper.ReservaMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static com.tenisahora.modules.reserva.DatosDePrueba.AHORA;
import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.persona;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;

class ReservaMapperTest {

    private final ReservaMapper mapper = new ReservaMapper(new CalculadoraDeMontos(PROPIEDADES), PROPIEDADES, id -> new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));

    // ---------- Mapper ----------

    @Test
    void numeroDe_esElIdConSeisDigitos() {
        assertThat(ReservaMapper.numeroDe(123L)).isEqualTo("R-000123");
        assertThat(ReservaMapper.numeroDe(1L)).isEqualTo("R-000001");
    }

    @Test
    void aRespuesta_deUnaPendiente_traeLaSeniaYCuandoVence() {
        Reserva reserva = reserva(10L, EstadoReserva.PENDIENTE_PAGO, HOY.plusDays(3), LocalTime.of(18, 0));
        ReservaJugador titular = new ReservaJugador();
        titular.setSocio(persona(3L, true));
        titular.setEsTitular(true);
        ReservaJugador invitado = new ReservaJugador();
        invitado.setNombre("Luciana");
        invitado.setApellido("Fernández");
        invitado.setEsTitular(false);

        ReservaResponse respuesta = mapper.aRespuesta(reserva, List.of(titular, invitado), List.of());

        assertThat(respuesta.montoSenia()).isEqualByComparingTo("8000.00");
        assertThat(respuesta.venceEn()).isEqualTo(AHORA.minusMinutes(5).plusMinutes(30));
        assertThat(respuesta.cancha()).isEqualTo("Cancha 1");
        assertThat(respuesta.jugadores().get(0).invitado()).isFalse();
        assertThat(respuesta.jugadores().get(0).personaId()).isEqualTo(3L);
        assertThat(respuesta.jugadores().get(0).documento()).isEqualTo("30000003");
        assertThat(respuesta.jugadores().get(1).invitado()).isTrue();
        assertThat(respuesta.jugadores().get(1).personaId()).isNull();
        assertThat(respuesta.jugadores().get(1).nombre()).isEqualTo("Luciana");
    }

    @Test
    void aRespuesta_deUnaConfirmadaOCancelada_noTieneVencimiento() {
        assertThat(mapper.aRespuesta(reserva(10L, EstadoReserva.CONFIRMADA, HOY, LocalTime.of(18, 0)), List.of(), List.of()).venceEn()).isNull();
        assertThat(mapper.aRespuesta(reserva(10L, EstadoReserva.CANCELADA, HOY, LocalTime.of(18, 0)), List.of(), List.of()).venceEn()).isNull();
    }
}
