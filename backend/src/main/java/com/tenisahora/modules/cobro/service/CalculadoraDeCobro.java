package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.reserva.service.CalculadoraDeMontos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** El monto del cobro de la seña lo calcula siempre el servidor (RD09, A04): nunca lo manda el cliente. */
@Component
@RequiredArgsConstructor
public class CalculadoraDeCobro {

    private final CalculadoraDeMontos calculadoraDeMontos;
    private final CalculoDeDescuentos descuentos;

    public record MontoDeCobro(BigDecimal montoBase, BigDecimal montoFinal, List<String> descuentosAplicados) {
    }

    /** La seña (RD09): 50% del total de la reserva. No depende de que el cobro ya tenga id. */
    public BigDecimal montoBase(BigDecimal montoTotalReserva) {
        return calculadoraDeMontos.senia(montoTotalReserva);
    }

    /**
     * Aplica los descuentos vigentes del socio sobre la base ya calculada (RD21). Pide el id del cobro porque el
     * módulo de descuentos deja registrado qué descuentos aplicó, así que el cobro tiene que estar ya persistido.
     * Sin socio o sin descuentos cargados, el monto final es igual al base.
     */
    public MontoDeCobro aplicarDescuentos(Long cobroId, BigDecimal montoBase, Long socioId) {
        CalculoDeDescuentos.ResultadoDeDescuento resultado = descuentos.calcular(cobroId, montoBase, socioId);
        return new MontoDeCobro(montoBase, resultado.montoFinal(), resultado.codigosAplicados());
    }
}
