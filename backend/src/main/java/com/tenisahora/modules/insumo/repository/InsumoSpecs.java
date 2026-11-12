package com.tenisahora.modules.insumo.repository;

import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.shared.util.Textos;
import org.springframework.data.jpa.domain.Specification;

/** Filtros combinables del listado: cada uno se agrega solo si el pedido lo trae, así no hay parámetros nulos en la consulta. */
public final class InsumoSpecs {

    private InsumoSpecs() {
    }

    public static Specification<Insumo> activo() {
        return (insumo, consulta, cb) -> cb.isTrue(insumo.get("activo"));
    }

    public static Specification<Insumo> deCategoria(Long categoriaId) {
        return (insumo, consulta, cb) -> cb.equal(insumo.get("categoria").get("id"), categoriaId);
    }

    /** El patrón ya viene sin tildes, en minúsculas y con los comodines escapados con '!' (ver Textos.patronContiene). */
    public static Specification<Insumo> nombreContiene(String patron) {
        return (insumo, consulta, cb) -> cb.like(
                cb.lower(cb.function("translate", String.class, insumo.get("nombre"),
                        cb.literal(Textos.LETRAS_CON_TILDE), cb.literal(Textos.LETRAS_SIN_TILDE))),
                patron, '!');
    }

    /** La misma regla que usa InsumoMapper para el campo estado de la respuesta. */
    public static Specification<Insumo> enEstado(EstadoStock estado) {
        return (insumo, consulta, cb) -> switch (estado) {
            case SIN_STOCK -> cb.equal(insumo.<Integer>get("cantidadDisponible"), 0);
            case STOCK_BAJO -> cb.and(
                    cb.greaterThan(insumo.<Integer>get("cantidadDisponible"), 0),
                    cb.lessThanOrEqualTo(insumo.<Integer>get("cantidadDisponible"), insumo.<Integer>get("cantidadMinima")));
            case DISPONIBLE -> cb.greaterThan(insumo.<Integer>get("cantidadDisponible"), insumo.<Integer>get("cantidadMinima"));
        };
    }
}
