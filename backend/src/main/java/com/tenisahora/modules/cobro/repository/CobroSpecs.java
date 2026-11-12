package com.tenisahora.modules.cobro.repository;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Filtros combinables del listado de cobros: cada uno se agrega solo si el pedido lo trae. */
public final class CobroSpecs {

    private CobroSpecs() {
    }

    public static Specification<Cobro> conId(Long id) {
        return (cobro, consulta, cb) -> cb.equal(cobro.get("id"), id);
    }

    public static Specification<Cobro> deReserva(Long reservaId) {
        return (cobro, consulta, cb) -> cb.equal(cobro.get("reserva").get("id"), reservaId);
    }

    public static Specification<Cobro> deConcepto(ConceptoCobro concepto) {
        return (cobro, consulta, cb) -> cb.equal(cobro.get("concepto"), concepto);
    }

    public static Specification<Cobro> deMedioPago(MedioPago medioPago) {
        return (cobro, consulta, cb) -> cb.equal(cobro.get("medioPago"), medioPago);
    }

    public static Specification<Cobro> desde(LocalDateTime desde) {
        return (cobro, consulta, cb) -> cb.greaterThanOrEqualTo(cobro.get("fecha"), desde);
    }

    public static Specification<Cobro> hasta(LocalDateTime hasta) {
        return (cobro, consulta, cb) -> cb.lessThan(cobro.get("fecha"), hasta);
    }
}
