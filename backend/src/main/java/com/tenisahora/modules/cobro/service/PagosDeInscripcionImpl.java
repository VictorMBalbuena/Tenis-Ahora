package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.service.PagosDeInscripcion;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class PagosDeInscripcionImpl implements PagosDeInscripcion {

    private static final LocalDateTime SIEMPRE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final CobroRepository cobroRepository;
    private final CalculadoraDeCobro calculadoraDeCobro;

    @Override
    public Resumen resumen(InscripcionClase inscripcion) {
        List<Cobro> pagados = cobroRepository.deInscripcionActividad(inscripcion.getId(), List.of(EstadoCobro.PAGADO), desdeDe(inscripcion))
                .stream().filter(cobro -> cobro.getConcepto() != ConceptoCobro.REEMBOLSO).toList();
        BigDecimal senia = inscripcion.getMontoTotal() != null ? calculadoraDeCobro.montoBase(inscripcion.getMontoTotal()) : BigDecimal.ZERO;
        return new Resumen(senia, sumar(pagados, Cobro::getMontoBase), sumar(pagados, Cobro::getMontoFinal));
    }

    static LocalDateTime desdeDe(InscripcionClase inscripcion) {
        return inscripcion.getFechaCreacion() != null ? inscripcion.getFechaCreacion() : SIEMPRE;
    }

    private BigDecimal sumar(List<Cobro> cobros, Function<Cobro, BigDecimal> monto) {
        return cobros.stream().map(monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
