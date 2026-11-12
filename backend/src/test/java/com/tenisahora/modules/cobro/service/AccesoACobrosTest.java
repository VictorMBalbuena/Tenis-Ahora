package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.reserva.service.AccesoAReservas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoACobrosTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);
    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);

    @Mock
    private AccesoAReservas accesoAReservas;
    @InjectMocks
    private AccesoACobros acceso;

    @Test
    void visible_elPersonal_veCualquierCobro() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");

        assertThat(acceso.visible(cobro, EMPLEADO)).isSameAs(cobro);
    }

    @Test
    void visible_unJugadorDeLaReserva_veElCobro() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");

        assertThat(acceso.visible(cobro, SOCIO)).isSameAs(cobro);
    }

    @Test
    void visible_unAjeno_404() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        when(accesoAReservas.visible(cobro.getReserva().getId(), SOCIO))
                .thenThrow(new RecursoNoEncontradoException(CodigoError.RESERVA_NO_ENCONTRADA, "Reserva no encontrada"));

        assertThatThrownBy(() -> acceso.visible(cobro, SOCIO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void visible_cobroSinReserva_elPersonalLoVe() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        cobro.setReserva(null);

        assertThat(acceso.visible(cobro, EMPLEADO)).isSameAs(cobro);
    }

    @Test
    void visible_cobroSinReservaDeOtroSocio_404() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        cobro.setReserva(null);

        assertThatThrownBy(() -> acceso.visible(cobro, SOCIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("COBRO_NO_ENCONTRADO");
    }
}
