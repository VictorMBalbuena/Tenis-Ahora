package com.tenisahora.modules.descuento.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.service.CalculoDeDescuentos;
import com.tenisahora.modules.cobro.service.CobroPorId;
import com.tenisahora.modules.descuento.entity.CobroDescuento;
import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.repository.CobroDescuentoRepository;
import com.tenisahora.modules.descuento.repository.DescuentoRepository;
import com.tenisahora.modules.reserva.entity.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qué hace el módulo de descuentos cuando se cobra la seña de una reserva.
 *
 * Antes esta clase aplicaba todos los descuentos activos a cualquier cobro de un socio. Ahora el descuento se
 * aplica por código sobre el total de la reserva, así que acá **el monto ya no se toca**: volver a descontar
 * cobraría el beneficio dos veces. Lo único que queda es dejar registrado qué descuento se usó, que es de
 * donde sale el "veces aplicado" y la regla de no borrar un descuento en uso.
 */
@ExtendWith(MockitoExtension.class)
class CalculoDeDescuentosImplTest {

    @Mock
    private DescuentoRepository descuentoRepository;
    @Mock
    private CobroDescuentoRepository cobroDescuentoRepository;
    @Mock
    private CobroPorId cobroPorId;
    @InjectMocks
    private CalculoDeDescuentosImpl calculo;

    private static final BigDecimal BASE = new BigDecimal("8000.00");

    private Cobro cobroDe(Reserva reserva) {
        Cobro cobro = new Cobro();
        cobro.setId(7L);
        cobro.setReserva(reserva);
        return cobro;
    }

    private Reserva reservaCon(String codigo) {
        Reserva reserva = new Reserva();
        reserva.setId(3L);
        reserva.setCodigoDescuento(codigo);
        return reserva;
    }

    private Descuento descuento(String codigo) {
        Descuento d = new Descuento();
        d.setId(11L);
        d.setCodigo(codigo);
        d.setTipo(TipoDescuento.PROMOCIONAL);
        d.setPorcentaje(new BigDecimal("25.00"));
        d.setActivo(true);
        return d;
    }

    @Test
    @DisplayName("Un cobro que no es de una reserva no lleva descuento")
    void cobroSinReserva() {
        when(cobroPorId.buscar(7L)).thenReturn(cobroDe(null));

        CalculoDeDescuentos.ResultadoDeDescuento resultado = calculo.calcular(7L, BASE, 5L);

        assertThat(resultado.montoFinal()).isEqualByComparingTo(BASE);
        assertThat(resultado.codigosAplicados()).isEmpty();
        verify(cobroDescuentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una reserva sin código no lleva descuento")
    void reservaSinCodigo() {
        when(cobroPorId.buscar(7L)).thenReturn(cobroDe(reservaCon(null)));

        CalculoDeDescuentos.ResultadoDeDescuento resultado = calculo.calcular(7L, BASE, 5L);

        assertThat(resultado.montoFinal()).isEqualByComparingTo(BASE);
        assertThat(resultado.codigosAplicados()).isEmpty();
        verify(cobroDescuentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Con código, se registra el descuento pero el monto no se vuelve a descontar")
    void conCodigoRegistraSinDescontarDeNuevo() {
        when(cobroPorId.buscar(7L)).thenReturn(cobroDe(reservaCon("CYBERWEEK")));
        when(descuentoRepository.findByCodigoIgnoreCase("CYBERWEEK")).thenReturn(Optional.of(descuento("CYBERWEEK")));
        when(cobroDescuentoRepository.existsByCobroIdAndDescuentoId(7L, 11L)).thenReturn(false);

        CalculoDeDescuentos.ResultadoDeDescuento resultado = calculo.calcular(7L, BASE, 5L);

        // El total de la reserva ya venía descontado: la seña se calculó sobre ese total.
        assertThat(resultado.montoFinal()).isEqualByComparingTo(BASE);
        assertThat(resultado.codigosAplicados()).containsExactly("CYBERWEEK");

        ArgumentCaptor<CobroDescuento> captor = ArgumentCaptor.forClass(CobroDescuento.class);
        verify(cobroDescuentoRepository).save(captor.capture());
        assertThat(captor.getValue().getDescuento().getCodigo()).isEqualTo("CYBERWEEK");
    }

    @Test
    @DisplayName("No se registra dos veces el mismo descuento sobre el mismo cobro")
    void noRegistraDosVeces() {
        when(cobroPorId.buscar(7L)).thenReturn(cobroDe(reservaCon("CYBERWEEK")));
        when(descuentoRepository.findByCodigoIgnoreCase("CYBERWEEK")).thenReturn(Optional.of(descuento("CYBERWEEK")));
        when(cobroDescuentoRepository.existsByCobroIdAndDescuentoId(7L, 11L)).thenReturn(true);

        CalculoDeDescuentos.ResultadoDeDescuento resultado = calculo.calcular(7L, BASE, 5L);

        assertThat(resultado.codigosAplicados()).containsExactly("CYBERWEEK");
        verify(cobroDescuentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si el descuento se borró después de reservar, el cobro sigue adelante")
    void codigoDeUnDescuentoBorrado() {
        when(cobroPorId.buscar(7L)).thenReturn(cobroDe(reservaCon("VIEJO")));
        when(descuentoRepository.findByCodigoIgnoreCase("VIEJO")).thenReturn(Optional.empty());

        CalculoDeDescuentos.ResultadoDeDescuento resultado = calculo.calcular(7L, BASE, 5L);

        assertThat(resultado.montoFinal()).isEqualByComparingTo(BASE);
        assertThat(resultado.codigosAplicados()).isEmpty();
        verify(cobroDescuentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ya no se aplican solos los descuentos activos del catálogo")
    void noAplicaElCatalogoCompleto() {
        when(cobroPorId.buscar(7L)).thenReturn(cobroDe(reservaCon(null)));

        calculo.calcular(7L, BASE, 5L);

        verify(descuentoRepository, never()).findByActivoTrue();
    }
}
