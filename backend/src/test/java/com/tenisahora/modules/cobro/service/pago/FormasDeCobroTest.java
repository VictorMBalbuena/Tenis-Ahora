package com.tenisahora.modules.cobro.service.pago;

import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tenisahora.modules.cobro.DatosDePrueba.PROPIEDADES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormasDeCobroTest {

    private final PagoDebito debito = new PagoDebito();
    private final PagoCredito credito = new PagoCredito(PROPIEDADES);
    private final FormasDeCobro formas = new FormasDeCobro(List.of(debito, credito));

    @Test
    void de_resuelveLaStrategyPorElMedio() {
        assertThat(formas.de(MedioPago.DEBITO)).isSameAs(debito);
        assertThat(formas.de(MedioPago.CREDITO)).isSameAs(credito);
    }

    @Test
    void de_unMedioSinImplementacion_errorClaro() {
        assertThatThrownBy(() -> formas.de(MedioPago.QR))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("MEDIO_DE_PAGO_INVALIDO");
    }
}
