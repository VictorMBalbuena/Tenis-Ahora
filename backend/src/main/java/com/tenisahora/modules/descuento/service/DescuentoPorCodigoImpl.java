package com.tenisahora.modules.descuento.service;

import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.model.CodigoDeDescuento;
import com.tenisahora.modules.descuento.repository.DescuentoRepository;
import com.tenisahora.modules.reserva.service.DescuentoPorCodigo;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Resuelve un código de descuento contra un monto (RF69).
 *
 * <p>Implementa el puerto que declara {@code reserva}. El cálculo vive acá y no en la pantalla: el frontend lo
 * usa solo para mostrar una vista previa, y la reserva vuelve a pedirlo al guardarse, así el monto que termina
 * cobrándose nunca depende de lo que haya mandado el cliente (RD09).
 */
@Service
@RequiredArgsConstructor
public class DescuentoPorCodigoImpl implements DescuentoPorCodigo {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final DescuentoRepository repositorio;

    @Override
    @Transactional(readOnly = true)
    public DescuentoAplicado aplicar(String codigo, BigDecimal montoBase) {
        Descuento descuento = buscarVigente(codigo);
        BigDecimal base = montoBase == null ? BigDecimal.ZERO : montoBase;

        BigDecimal descontado = acotar(descontarDe(base, descuento), base);
        BigDecimal final_ = base.subtract(descontado).setScale(2, RoundingMode.HALF_UP);

        return new DescuentoAplicado(descuento.getCodigo(), describir(descuento), descontado, final_);
    }

    /** Un código que no existe y uno desactivado son lo mismo para quien reserva: no se puede usar. */
    private Descuento buscarVigente(String codigo) {
        return repositorio.findByCodigoIgnoreCase(CodigoDeDescuento.normalizar(codigo))
                .filter(d -> Boolean.TRUE.equals(d.getActivo()))
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.NO_ENCONTRADO,
                        "El código de descuento no existe o no está vigente"));
    }

    private BigDecimal descontarDe(BigDecimal base, Descuento descuento) {
        if (descuento.getPorcentaje() != null) {
            return base.multiply(descuento.getPorcentaje()).divide(CIEN, 2, RoundingMode.HALF_UP);
        }
        return descuento.getMontoFijo() == null ? BigDecimal.ZERO : descuento.getMontoFijo();
    }

    /** Un monto fijo mayor que el total no deja la reserva en negativo: como mucho la deja en cero. */
    private BigDecimal acotar(BigDecimal descontado, BigDecimal base) {
        BigDecimal sinNegativos = descontado.max(BigDecimal.ZERO);
        return sinNegativos.min(base).setScale(2, RoundingMode.HALF_UP);
    }

    private String describir(Descuento descuento) {
        if (descuento.getPorcentaje() != null) {
            return descuento.getPorcentaje().stripTrailingZeros().toPlainString() + "% de descuento";
        }
        return "$" + descuento.getMontoFijo().stripTrailingZeros().toPlainString() + " de descuento";
    }
}
