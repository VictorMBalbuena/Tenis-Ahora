package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.modules.reserva.dto.JugadorRequest;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.modules.reserva.entity.ReservaJugador;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.tenisahora.modules.reserva.DatosDePrueba.persona;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ArmadoDeJugadoresTest {

    @Mock
    private SocioRepository socioRepository;
    @InjectMocks
    private ArmadoDeJugadores armado;

    private final Reserva reserva = new Reserva();
    private final Socio titular = persona(3L, true);

    private static JugadorRequest registrado(Long id) {
        return new JugadorRequest(id, null, null, null);
    }

    private static JugadorRequest invitado(String nombre, String apellido, String documento) {
        return new JugadorRequest(null, nombre, apellido, documento);
    }

    private void personasExistentes(Socio... personas) {
        lenient().when(socioRepository.findAllById(org.mockito.ArgumentMatchers.anyIterable())).thenReturn(List.of(personas));
    }

    @Test
    void armar_poneAlTitularPrimeroYLuegoLosAcompaniantes() {
        personasExistentes(persona(4L, true));

        List<ReservaJugador> jugadores = armado.armar(reserva, titular, List.of(registrado(4L), invitado("Luciana", "Fernández", "30123456")));

        assertThat(jugadores).hasSize(3);
        assertThat(jugadores.get(0).getEsTitular()).isTrue();
        assertThat(jugadores.get(0).getSocio()).isSameAs(titular);
        assertThat(jugadores.get(1).getSocio().getId()).isEqualTo(4L);
        assertThat(jugadores.get(1).getEsTitular()).isFalse();
        assertThat(jugadores.get(2).getSocio()).isNull();
        assertThat(jugadores.get(2).getNombre()).isEqualTo("Luciana");
        assertThat(jugadores.get(2).getDocumento()).isEqualTo("30123456");
        assertThat(jugadores).allSatisfy(jugador -> assertThat(jugador.getReserva()).isSameAs(reserva));
    }

    @Test
    void armar_elDocumentoDelInvitadoEsOpcional() {
        personasExistentes();

        List<ReservaJugador> jugadores = armado.armar(reserva, titular, List.of(invitado("Luciana", "Fernández", "  ")));

        assertThat(jugadores.get(1).getDocumento()).isNull();
    }

    @Test
    void armar_invitadoSinNombreOSinApellido_400() {
        personasExistentes();

        assertThatThrownBy(() -> armado.armar(reserva, titular, List.of(invitado("Luciana", null, null))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("nombre y apellido")
                .extracting("codigo").isEqualTo("RESERVA_JUGADORES_INVALIDOS");
        assertThatThrownBy(() -> armado.armar(reserva, titular, List.of(invitado("  ", "Fernández", null))))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void armar_conUnaPersonaQueNoExiste_400() {
        personasExistentes();

        assertThatThrownBy(() -> armado.armar(reserva, titular, List.of(registrado(99L))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("no está registrado");
    }

    @Test
    void armar_conUnaPersonaDeshabilitada_400() {
        personasExistentes(persona(4L, false));

        assertThatThrownBy(() -> armado.armar(reserva, titular, List.of(registrado(4L))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("deshabilitado");
    }

    @Test
    void armar_conElMismoJugadorDosVeces_400() {
        personasExistentes(persona(4L, true));

        assertThatThrownBy(() -> armado.armar(reserva, titular, List.of(registrado(4L), registrado(4L), invitado("A", "B", null))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("más de una vez");
    }

    @Test
    void armar_conElTitularComoAcompaniante_400() {
        personasExistentes(titular);

        assertThatThrownBy(() -> armado.armar(reserva, titular, List.of(registrado(3L))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("más de una vez");
    }
}
