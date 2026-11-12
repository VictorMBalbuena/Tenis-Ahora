package com.tenisahora.modules.descuento.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.service.CalculoDeDescuentos;
import com.tenisahora.modules.cobro.service.CobroPorId;
import com.tenisahora.modules.descuento.entity.CobroDescuento;
import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.repository.CobroDescuentoRepository;
import com.tenisahora.modules.descuento.repository.DescuentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementa el puerto que declara {@code cobro} (RD21): la inversión de dependencia evita el ciclo entre
 * módulos, ya que {@code descuento} ya importa a {@code cobro} por {@link CobroDescuento}.
 *
 * <p><b>Un descuento ya no se aplica solo.</b> Antes esta clase aplicaba todos los descuentos activos a
 * cualquier cobro de un socio, como provisorio hasta que existiera el modelo real. Ahora un descuento tiene
 * código y se aplica cuando alguien lo escribe al reservar: eso pasa en {@code ReservaServiceImpl}, sobre el
 * total de la reserva, y la seña se calcula sobre ese total ya descontado. Volver a descontar acá cobraría el
 * beneficio dos veces.
 *
 * <p>Lo que sí sigue haciendo es dejar registrado en {@code cobro_descuento} qué descuento se usó, que es de
 * donde salen el "veces aplicado" de la pantalla y la regla de no poder borrar un descuento ya usado.
 */
@Service
@RequiredArgsConstructor
public class CalculoDeDescuentosImpl implements CalculoDeDescuentos {

    private final DescuentoRepository descuentoRepository;
    private final CobroDescuentoRepository cobroDescuentoRepository;
    private final CobroPorId cobroPorId;

    @Override
    @Transactional
    public ResultadoDeDescuento calcular(Long cobroId, BigDecimal montoBase, Long socioId) {
        Cobro cobro = cobroPorId.buscar(cobroId);

        return descuentoDeLaReserva(cobro)
                .map(descuento -> registrar(cobro, descuento, montoBase))
                .orElseGet(() -> new ResultadoDeDescuento(montoBase, List.of()));
    }

    /**
     * El descuento que se usó al reservar, si el cobro es de una reserva y esa reserva tenía código.
     *
     * Se busca por el código guardado en la reserva y no por una referencia, porque reserva a propósito no
     * depende de este módulo. Si después el descuento se borró, no hay nada que registrar.
     */
    private Optional<Descuento> descuentoDeLaReserva(Cobro cobro) {
        return Optional.ofNullable(cobro.getReserva())
                .map(reserva -> reserva.getCodigoDescuento())
                .filter(codigo -> !codigo.isBlank())
                .flatMap(descuentoRepository::findByCodigoIgnoreCase);
    }

    /**
     * Deja constancia de que ese cobro salió de una reserva con descuento. El monto no se toca: ya viene
     * descontado desde el total de la reserva.
     */
    private ResultadoDeDescuento registrar(Cobro cobro, Descuento descuento, BigDecimal montoBase) {
        if (!cobroDescuentoRepository.existsByCobroIdAndDescuentoId(cobro.getId(), descuento.getId())) {
            CobroDescuento registro = new CobroDescuento();
            registro.setCobro(cobro);
            registro.setDescuento(descuento);
            cobroDescuentoRepository.save(registro);
        }
        return new ResultadoDeDescuento(montoBase, List.of(descuento.getCodigo()));
    }
}
