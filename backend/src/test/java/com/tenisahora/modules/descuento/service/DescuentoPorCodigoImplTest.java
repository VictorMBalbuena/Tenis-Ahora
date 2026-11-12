package com.tenisahora.modules.descuento.service;

import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.repository.DescuentoRepository;
import com.tenisahora.modules.reserva.service.DescuentoPorCodigo;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Resolver un código de descuento contra un monto (RF69). */
@ExtendWith(MockitoExtension.class)
class DescuentoPorCodigoImplTest {

    @Mock
    private DescuentoRepository repositorio;

    @InjectMocks
    private DescuentoPorCodigoImpl servicio;

    private static Descuento conPorcentaje(String codigo, String porcentaje, boolean activo) {
        Descuento d = new Descuento();
        d.setCodigo(codigo);
        d.setTipo(TipoDescuento.PROMOCIONAL);
        d.setPorcentaje(new BigDecimal(porcentaje));
        d.setActivo(activo);
        return d;
    }

    private static Descuento conMontoFijo(String codigo, String monto) {
        Descuento d = new Descuento();
        d.setCodigo(codigo);
        d.setTipo(TipoDescuento.PROMOCIONAL);
        d.setMontoFijo(new BigDecimal(monto));
        d.setActivo(true);
        return d;
    }

    @Test
    @DisplayName("El ejemplo del pedido: CYBERWEEK descuenta el 25% del total")
    void porcentajeSobreElTotal() {
        when(repositorio.findByCodigoIgnoreCase("CYBERWEEK"))
                .thenReturn(Optional.of(conPorcentaje("CYBERWEEK", "25.00", true)));

        DescuentoPorCodigo.DescuentoAplicado aplicado = servicio.aplicar("CYBERWEEK", new BigDecimal("16000.00"));

        assertThat(aplicado.montoDescontado()).isEqualByComparingTo("4000.00");
        assertThat(aplicado.montoFinal()).isEqualByComparingTo("12000.00");
        assertThat(aplicado.descripcion()).isEqualTo("25% de descuento");
    }

    @Test
    @DisplayName("Da igual cómo se escriba: cyberweek y  CyberWeek  son el mismo código")
    void noDistingueMayusculasNiEspacios() {
        when(repositorio.findByCodigoIgnoreCase("CYBERWEEK"))
                .thenReturn(Optional.of(conPorcentaje("CYBERWEEK", "25.00", true)));

        assertThat(servicio.aplicar("  cyberweek ", new BigDecimal("16000.00")).codigo()).isEqualTo("CYBERWEEK");
    }

    @Test
    @DisplayName("Un monto fijo se resta tal cual")
    void montoFijo() {
        when(repositorio.findByCodigoIgnoreCase("MIL")).thenReturn(Optional.of(conMontoFijo("MIL", "1000.00")));

        DescuentoPorCodigo.DescuentoAplicado aplicado = servicio.aplicar("MIL", new BigDecimal("16000.00"));

        assertThat(aplicado.montoDescontado()).isEqualByComparingTo("1000.00");
        assertThat(aplicado.montoFinal()).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("Un monto fijo mayor al total deja la reserva en cero, nunca en negativo")
    void montoFijoMayorAlTotal() {
        when(repositorio.findByCodigoIgnoreCase("ENORME")).thenReturn(Optional.of(conMontoFijo("ENORME", "99000.00")));

        DescuentoPorCodigo.DescuentoAplicado aplicado = servicio.aplicar("ENORME", new BigDecimal("16000.00"));

        assertThat(aplicado.montoDescontado()).isEqualByComparingTo("16000.00");
        assertThat(aplicado.montoFinal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Un código que no existe se rechaza")
    void codigoInexistente() {
        when(repositorio.findByCodigoIgnoreCase("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aplicar("noexiste", new BigDecimal("16000.00")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigoError())
                        .isEqualTo(CodigoError.NO_ENCONTRADO));
    }

    @Test
    @DisplayName("Un código dado de baja se rechaza igual que uno inexistente")
    void codigoInactivo() {
        when(repositorio.findByCodigoIgnoreCase("VENCIDO"))
                .thenReturn(Optional.of(conPorcentaje("VENCIDO", "25.00", false)));

        assertThatThrownBy(() -> servicio.aplicar("VENCIDO", new BigDecimal("16000.00")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("El redondeo queda en dos decimales")
    void redondeo() {
        when(repositorio.findByCodigoIgnoreCase("TRES"))
                .thenReturn(Optional.of(conPorcentaje("TRES", "33.33", true)));

        DescuentoPorCodigo.DescuentoAplicado aplicado = servicio.aplicar("TRES", new BigDecimal("10000.00"));

        assertThat(aplicado.montoDescontado()).isEqualByComparingTo("3333.00");
        assertThat(aplicado.montoFinal()).isEqualByComparingTo("6667.00");
        assertThat(aplicado.montoFinal().scale()).isEqualTo(2);
    }
}
