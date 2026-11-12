package com.tenisahora.modules.cobro.repository;

import com.tenisahora.modules.cobro.entity.Recibo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Filtros combinables del listado de recibos: cada uno se agrega solo si el pedido lo trae. */
public final class ReciboSpecs {

    private ReciboSpecs() {
    }

    /** El número de recibo es el id del cobro que lo originó (ver CobroMapper.numeroDe). */
    public static Specification<Recibo> delCobro(Long cobroId) {
        return (recibo, consulta, cb) -> cb.equal(recibo.get("cobro").get("id"), cobroId);
    }

    public static Specification<Recibo> desde(LocalDateTime desde) {
        return (recibo, consulta, cb) -> cb.greaterThanOrEqualTo(recibo.get("fechaEmision"), desde);
    }

    public static Specification<Recibo> hasta(LocalDateTime hasta) {
        return (recibo, consulta, cb) -> cb.lessThan(recibo.get("fechaEmision"), hasta);
    }
}
