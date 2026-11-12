package com.tenisahora.modules.descuento.repository;

import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.shared.util.Textos;
import org.springframework.data.jpa.domain.Specification;

public final class DescuentoSpecs {
    private DescuentoSpecs() {}

    public static Specification<Descuento> porTipo(TipoDescuento tipo) {
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Descuento> activo(Boolean activo) {
        return (root, query, cb) -> cb.equal(root.get("activo"), activo);
    }

    public static Specification<Descuento> condicionesContiene(String busqueda) {
        return (root, query, cb) -> cb.like(
                cb.lower(cb.function("translate", String.class, root.get("condiciones"),
                        cb.literal(Textos.LETRAS_CON_TILDE), cb.literal(Textos.LETRAS_SIN_TILDE))),
                Textos.patronContiene(busqueda), '!');
    }
}
