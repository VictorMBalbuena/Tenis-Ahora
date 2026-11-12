package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.repository.ReservaJugadorRepository;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.Optional;

import static com.tenisahora.modules.reserva.DatosDePrueba.HOY;
import static com.tenisahora.modules.reserva.DatosDePrueba.reserva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoAReservasTest {

    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);
    private static final Actor ADMIN = new Actor(1L, Rol.ADMINISTRADOR);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaJugadorRepository jugadorRepository;
    @InjectMocks
    private AccesoAReservas acceso;

    private final Reserva reserva = reserva(10L, EstadoReserva.CONFIRMADA, HOY.plusDays(3), LocalTime.of(18, 0));

    @Test
    void esPersonal_soloEmpleadoYAdministrador() {
        assertThat(AccesoAReservas.esPersonal(EMPLEADO)).isTrue();
        assertThat(AccesoAReservas.esPersonal(ADMIN)).isTrue();
        assertThat(AccesoAReservas.esPersonal(SOCIO)).isFalse();
        assertThat(AccesoAReservas.esPersonal(new Actor(4L, Rol.PROFESOR))).isFalse();
    }

    @Test
    void visible_paraElPersonal_noPreguntaPorLaParticipacion() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));

        assertThat(acceso.visible(10L, EMPLEADO)).isSameAs(reserva);
        verify(jugadorRepository, never()).existsByReservaIdAndSocioId(10L, 2L);
    }

    @Test
    void visible_paraUnSocioQueParticipa() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(jugadorRepository.existsByReservaIdAndSocioId(10L, 3L)).thenReturn(true);

        assertThat(acceso.visible(10L, SOCIO)).isSameAs(reserva);
    }

    @Test
    void visible_paraUnSocioQueNoParticipa_404_comoSiNoExistiera() {
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(jugadorRepository.existsByReservaIdAndSocioId(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> acceso.visible(10L, SOCIO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void visible_deUnaReservaInexistente_404() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceso.visible(99L, EMPLEADO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void paraCancelar_elPersonalCancelaCualquiera_conOSinBloqueo() {
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));

        assertThat(acceso.paraCancelar(10L, EMPLEADO, true)).isSameAs(reserva);
        assertThat(acceso.paraCancelar(10L, ADMIN, false)).isSameAs(reserva);
        verify(reservaRepository).buscarParaModificar(10L);
        verify(reservaRepository).findById(10L);
    }

    @Test
    void paraCancelar_elSocioTitularPuede() {
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        when(jugadorRepository.existsByReservaIdAndEsTitularTrueAndSocioId(10L, 3L)).thenReturn(true);

        assertThat(acceso.paraCancelar(10L, SOCIO, true)).isSameAs(reserva);
    }

    @Test
    void paraCancelar_unJugadorQueNoEsTitular_403() {
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        when(jugadorRepository.existsByReservaIdAndEsTitularTrueAndSocioId(10L, 3L)).thenReturn(false);
        when(jugadorRepository.existsByReservaIdAndSocioId(10L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> acceso.paraCancelar(10L, SOCIO, true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("titular");
    }

    @Test
    void paraCancelar_unSocioAjeno_404() {
        when(reservaRepository.buscarParaModificar(10L)).thenReturn(Optional.of(reserva));
        when(jugadorRepository.existsByReservaIdAndEsTitularTrueAndSocioId(10L, 3L)).thenReturn(false);
        when(jugadorRepository.existsByReservaIdAndSocioId(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> acceso.paraCancelar(10L, SOCIO, true)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void paraCancelar_deUnaReservaInexistente_404() {
        when(reservaRepository.buscarParaModificar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceso.paraCancelar(99L, EMPLEADO, true)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
