package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.PagosDeReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class PagosDeReservaImpl implements PagosDeReserva {

    private final CobroRepository cobroRepository;

    @Override
    public Resumen resumen(Long reservaId) {
        List<Cobro> pagados = cobroRepository.findByReservaIdAndEstado(reservaId, EstadoCobro.PAGADO).stream()
                .filter(cobro -> cobro.getConcepto() != ConceptoCobro.REEMBOLSO)
                .toList();
        return new Resumen(sumar(pagados, Cobro::getMontoBase), sumar(pagados, Cobro::getMontoFinal));
    }

    private BigDecimal sumar(List<Cobro> cobros, Function<Cobro, BigDecimal> monto) {
        return cobros.stream().map(monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
