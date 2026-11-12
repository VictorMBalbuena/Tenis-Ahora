package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagoDebitoTest {

    private final PagoDebito pago = new PagoDebito();

    @Test
    void medio_debito() {
        assertThat(pago.medio()).isEqualTo(MedioPago.DEBITO);
    }

    @Test
    void cobrar_aprobado_dejaPagadoConElNumeroDeOperacion() {
        ResultadoDeCobro resultado = pago.cobrar(new Cobro(), new DatosDeCobro("000123", null, true, 2L));

        assertThat(resultado.estado()).isEqualTo(EstadoCobro.PAGADO);
        assertThat(resultado.numeroOperacion()).isEqualTo("000123");
    }

    @Test
    void cobrar_rechazado_A1_dejaRechazado() {
        ResultadoDeCobro resultado = pago.cobrar(new Cobro(), new DatosDeCobro("000123", null, false, 2L));

        assertThat(resultado.estado()).isEqualTo(EstadoCobro.RECHAZADO);
    }

    @Test
    void cobrar_sinNumeroDeOperacion_fallaConSolicitudInvalida() {
        assertThatThrownBy(() -> pago.cobrar(new Cobro(), new DatosDeCobro(" ", null, true, 2L)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("NUMERO_DE_OPERACION_REQUERIDO");
    }
}
