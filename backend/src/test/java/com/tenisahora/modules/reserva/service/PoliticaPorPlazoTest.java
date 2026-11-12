package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.tenisahora.modules.reserva.DatosDePrueba.AHORA;
import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.PROPIEDADES;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;

/** La política de cancelación (RD12, RD13), con las reglas por defecto: plazo de 6 horas, seña del 50% y reembolso total a tiempo. */
class PoliticaPorPlazoTest {

    private final CalculadoraDeMontos calculadora = new CalculadoraDeMontos(PROPIEDADES);
    private final PoliticaPorPlazo politica = new PoliticaPorPlazo(PROPIEDADES, calculadora, id -> new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(new java.math.BigDecimal("8000.00"), new java.math.BigDecimal("8000.00")));

    private static Reserva reservaEn(EstadoReserva estado, LocalDateTime inicio) {
        return reserva(1L, estado, inicio.toLocalDate(), inicio.toLocalTime());
    }

    // ---------- Política ----------

    @Test
    void confirmadaConMuchaAntelacion_seDevuelveLaSenia_RD13() {
        ResultadoDeCancelacion resultado = politica.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.plusHours(30)), AHORA);

        assertThat(resultado.aTiempo()).isTrue();
        assertThat(resultado.horasDeAntelacion()).isEqualTo(30);
        assertThat(resultado.montoReembolso()).isEqualByComparingTo("8000.00");
        assertThat(resultado.montoPenalidad()).isEqualByComparingTo("0");
    }

    @Test
    void elReembolsoSaleDeLoCobrado_noDelTotalRecalculado() {
        PoliticaPorPlazo conPocoCobrado = new PoliticaPorPlazo(PROPIEDADES, calculadora,
                id -> new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(new java.math.BigDecimal("4000.00"), new java.math.BigDecimal("4000.00")));

        assertThat(conPocoCobrado.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.plusHours(30)), AHORA).montoReembolso()).isEqualByComparingTo("4000.00");
    }

    @Test
    void confirmadaConExactamenteSeisHoras_esATiempo() {
        assertThat(politica.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.plusHours(6)), AHORA).aTiempo()).isTrue();
    }

    @Test
    void confirmadaAUnMinutoDelPlazo_esTardiaYPierdeLaSenia_RD12() {
        ResultadoDeCancelacion resultado = politica.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.plusHours(6).minusMinutes(1)), AHORA);

        assertThat(resultado.aTiempo()).isFalse();
        assertThat(resultado.montoPenalidad()).isEqualByComparingTo("8000.00");
        assertThat(resultado.montoReembolso()).isEqualByComparingTo("0");
    }

    @Test
    void confirmadaQueYaEmpezo_esTardia() {
        assertThat(politica.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.minusHours(1)), AHORA).aTiempo()).isFalse();
    }

    @Test
    void pendienteDePago_noTieneNadaQueDevolverNiQuePerder() {
        ResultadoDeCancelacion aTiempo = politica.evaluar(reservaEn(EstadoReserva.PENDIENTE_PAGO, AHORA.plusHours(30)), AHORA);
        ResultadoDeCancelacion tarde = politica.evaluar(reservaEn(EstadoReserva.PENDIENTE_PAGO, AHORA.plusHours(1)), AHORA);

        assertThat(aTiempo.montoPenalidad()).isEqualByComparingTo("0");
        assertThat(aTiempo.montoReembolso()).isEqualByComparingTo("0");
        assertThat(tarde.montoPenalidad()).isEqualByComparingTo("0");
        assertThat(tarde.montoReembolso()).isEqualByComparingTo("0");
        assertThat(tarde.aTiempo()).isFalse();
    }

    @Test
    void elPorcentajeDeReembolsoYElPlazoSonConfigurables() {
        ReservasProperties otras = new ReservasProperties(30, 2, 24, 50, 50, 30, 30, 30, 10, 20);
        PoliticaPorPlazo otraPolitica = new PoliticaPorPlazo(otras, new CalculadoraDeMontos(otras), id -> new com.tenisahora.modules.reserva.service.PagosDeReserva.Resumen(new java.math.BigDecimal("8000.00"), new java.math.BigDecimal("8000.00")));

        ResultadoDeCancelacion conDiezHoras = otraPolitica.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.plusHours(10)), AHORA);
        ResultadoDeCancelacion conTreintaHoras = otraPolitica.evaluar(reservaEn(EstadoReserva.CONFIRMADA, AHORA.plusHours(30)), AHORA);

        assertThat(conDiezHoras.aTiempo()).isFalse();
        assertThat(conTreintaHoras.aTiempo()).isTrue();
        assertThat(conTreintaHoras.montoReembolso()).isEqualByComparingTo("4000.00");
    }

    @Test
    void usaElDiaDeLaReservaYNoSoloLaHora() {
        // mañana a las 9: faltan 21 horas, no 0
        assertThat(politica.evaluar(reserva(1L, EstadoReserva.CONFIRMADA, HOY.plusDays(1), java.time.LocalTime.of(9, 0)), AHORA).horasDeAntelacion())
                .isEqualTo(21);
    }
}
