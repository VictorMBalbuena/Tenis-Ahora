package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CobroDeInscripcionTorneoTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);
    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final CobroRequest EFECTIVO = new CobroRequest(MedioPago.EFECTIVO, null, null, true);
    private static final CobroRequest QR = new CobroRequest(MedioPago.QR, null, null, true);

    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private ConfirmacionDeInscripcionTorneo confirmacion;
    @Mock
    private EjecucionDeCobro ejecucion;
    @Mock
    private ReemplazoDeQr reemplazoDeQr;
    @Mock
    private CobroMapper cobroMapper;
    @InjectMocks
    private CobroDeInscripcionTorneo cobroDeTorneo;

    private Inscripcion inscripcion(EstadoInscripcionTorneo estado) {
        Socio jugador = mock(Socio.class);
        when(jugador.getId()).thenReturn(3L);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(9L);
        inscripcion.setEstado(estado);
        inscripcion.setJugador1(jugador);
        inscripcion.setMontoTotal(new BigDecimal("5000.00"));
        when(inscripcionRepository.findById(9L)).thenReturn(Optional.of(inscripcion));
        when(ejecucion.nuevo(any(), any(), any())).thenReturn(new Cobro());
        return inscripcion;
    }

    @Test
    void registrar_inscripcionInexistente_404() {
        when(inscripcionRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cobroDeTorneo.registrar(9L, EFECTIVO, EMPLEADO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registrar_unSocioQueNoParticipa_404() {
        inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);

        assertThatThrownBy(() -> cobroDeTorneo.registrar(9L, QR, new Actor(99L, Rol.SOCIO))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registrar_laParejaTambienPuedePagar() {
        Inscripcion inscripcion = inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        Socio pareja = mock(Socio.class);
        when(pareja.getId()).thenReturn(99L);
        inscripcion.setPareja(pareja);

        cobroDeTorneo.registrar(9L, QR, new Actor(99L, Rol.SOCIO));

        verify(ejecucion).asentar(any(), eq(new BigDecimal("5000.00")), eq(3L), eq(QR), any());
    }

    @Test
    void registrar_unSocioConEfectivo_403() {
        inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        doThrow(new AccessDeniedException("Solo el personal")).when(ejecucion).asegurarMedioPermitido(MedioPago.EFECTIVO, SOCIO);

        assertThatThrownBy(() -> cobroDeTorneo.registrar(9L, EFECTIVO, SOCIO)).isInstanceOf(AccessDeniedException.class);

        verify(ejecucion, never()).asentar(any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoInscripcionTorneo.class, names = {"CONFIRMADA", "CANCELADA"})
    void registrar_sinArancelPendiente_422(EstadoInscripcionTorneo estado) {
        inscripcion(estado);

        assertThatThrownBy(() -> cobroDeTorneo.registrar(9L, EFECTIVO, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSCRIPCION_NO_COBRABLE");
    }

    @Test
    void registrar_conQrEnCurso_reutilizaElCobro() {
        inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        Cobro enCurso = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "5000.00");
        when(cobroRepository.deInscripcionTorneo(9L, List.of(EstadoCobro.PENDIENTE))).thenReturn(List.of(enCurso));

        cobroDeTorneo.registrar(9L, QR, SOCIO);

        verify(cobroMapper).aRespuesta(enCurso);
        verify(ejecucion, never()).asentar(any(), any(), any(), any(), any());
    }

    @Test
    void registrar_otroMedioConQrEnCurso_reemplazaElQrYCobraElArancelCompleto() {
        inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        Cobro enCurso = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "5000.00");
        when(cobroRepository.deInscripcionTorneo(9L, List.of(EstadoCobro.PENDIENTE))).thenReturn(List.of(enCurso));

        cobroDeTorneo.registrar(9L, EFECTIVO, EMPLEADO);

        verify(reemplazoDeQr).reemplazar(enCurso, EMPLEADO);
        verify(ejecucion).nuevo(MedioPago.EFECTIVO, ConceptoCobro.INSCRIPCION, EMPLEADO);
        verify(ejecucion).asentar(any(), eq(new BigDecimal("5000.00")), eq(3L), eq(EFECTIVO), any());
    }

    @Test
    void registrar_conQrRechazado_cierraSuLinkAntesDeCobrar() {
        inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        List<Cobro> rechazados = List.of(cobro(7L, EstadoCobro.RECHAZADO, MedioPago.QR, "5000.00"));
        when(cobroRepository.deInscripcionTorneo(9L, List.of(EstadoCobro.RECHAZADO))).thenReturn(rechazados);

        cobroDeTorneo.registrar(9L, EFECTIVO, EMPLEADO);

        verify(reemplazoDeQr).reemplazarRechazados(rechazados, EMPLEADO);
    }

    @Test
    void registrar_siElQrReemplazadoYaEstabaPago_422YNoCobraDosVeces() {
        Inscripcion inscripcion = inscripcion(EstadoInscripcionTorneo.PENDIENTE_PAGO);
        Cobro enCurso = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "5000.00");
        when(cobroRepository.deInscripcionTorneo(9L, List.of(EstadoCobro.PENDIENTE))).thenReturn(List.of(enCurso));
        doAnswer(inv -> {
            inscripcion.setEstado(EstadoInscripcionTorneo.CONFIRMADA);
            return null;
        }).when(reemplazoDeQr).reemplazar(enCurso, EMPLEADO);

        assertThatThrownBy(() -> cobroDeTorneo.registrar(9L, EFECTIVO, EMPLEADO)).isInstanceOf(ReglaDeNegocioException.class);

        verify(ejecucion, never()).asentar(any(), any(), any(), any(), any());
    }
}
