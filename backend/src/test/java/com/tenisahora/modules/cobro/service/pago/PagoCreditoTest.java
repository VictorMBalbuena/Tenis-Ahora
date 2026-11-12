package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.service.CobrosProperties;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import static com.tenisahora.modules.cobro.DatosDePrueba.PROPIEDADES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagoCreditoTest {

    private final PagoCredito pago = new PagoCredito(PROPIEDADES);

    @Test
    void medio_credito() {
        assertThat(pago.medio()).isEqualTo(MedioPago.CREDITO);
    }

    @Test
    void cobrar_aprobado_dejaPagadoYGuardaLasCuotas() {
        Cobro cobro = new Cobro();

        ResultadoDeCobro resultado = pago.cobrar(cobro, new DatosDeCobro("000456", 6, true, 2L));

        assertThat(resultado.estado()).isEqualTo(EstadoCobro.PAGADO);
        assertThat(cobro.getCuotas()).isEqualTo(6);
    }

    @Test
    void cobrar_sinCuotas_usaUnaSolaCuota() {
        Cobro cobro = new Cobro();

        pago.cobrar(cobro, new DatosDeCobro("000456", null, true, 2L));

        assertThat(cobro.getCuotas()).isEqualTo(1);
    }

    @Test
    void cobrar_rechazado_A1_dejaRechazado() {
        ResultadoDeCobro resultado = pago.cobrar(new Cobro(), new DatosDeCobro("000456", 1, false, 2L));

        assertThat(resultado.estado()).isEqualTo(EstadoCobro.RECHAZADO);
    }

    @Test
    void cobrar_sinNumeroDeOperacion_fallaConSolicitudInvalida() {
        assertThatThrownBy(() -> pago.cobrar(new Cobro(), new DatosDeCobro(null, 1, true, 2L)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting("codigo").isEqualTo("NUMERO_DE_OPERACION_REQUERIDO");
    }

    @Test
    void cobrar_cuotasFueraDelMaximo_fallaConCuotasInvalidas() {
        assertThatThrownBy(() -> pago.cobrar(new Cobro(), new DatosDeCobro("000456", 13, true, 2L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CUOTAS_INVALIDAS");
    }

    @Test
    void cobrar_cuotasEnCero_fallaConCuotasInvalidas() {
        assertThatThrownBy(() -> pago.cobrar(new Cobro(), new DatosDeCobro("000456", 0, true, 2L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CUOTAS_INVALIDAS");
    }
}
