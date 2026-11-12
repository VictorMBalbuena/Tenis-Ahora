package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.entity.EstadoInscripcion;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.repository.InscripcionClaseRepository;
import com.tenisahora.modules.actividad.service.ConfirmacionDeInscripcion;
import com.tenisahora.modules.actividad.service.PagosDeInscripcion;
import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CobroDeInscripcionActividadTest {

    private static final Actor EMPLEADO = new Actor(1L, Rol.EMPLEADO);
    private static final Actor SOCIO = new Actor(3L, Rol.SOCIO);
    private static final CobroRequest EFECTIVO = new CobroRequest(MedioPago.EFECTIVO, null, null, true);
    private static final CobroRequest QR = new CobroRequest(MedioPago.QR, null, null, true);

    @Mock
    private InscripcionClaseRepository inscripcionRepository;
    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private PagosDeInscripcion pagos;
    @Mock
    private ConfirmacionDeInscripcion confirmacion;
    @Mock
    private EjecucionDeCobro ejecucion;
    @Mock
    private ReemplazoDeQr reemplazoDeQr;
    @Mock
    private CobroMapper cobroMapper;
    @InjectMocks
    private CobroDeInscripcionActividad cobroDeActividad;

    private InscripcionClase inscripcion(EstadoInscripcion estado, Boolean activa, String total, String cubierto) {
        Socio socio = mock(Socio.class);
        when(socio.getId()).thenReturn(3L);
        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setId(9L);
        inscripcion.setEstado(estado);
        inscripcion.setActiva(activa);
        inscripcion.setSocio(socio);
        inscripcion.setMontoTotal(total == null ? null : new BigDecimal(total));
        when(inscripcionRepository.conActividad(9L)).thenReturn(Optional.of(inscripcion));
        when(pagos.resumen(inscripcion)).thenReturn(new PagosDeInscripcion.Resumen(new BigDecimal("5000.00"), new BigDecimal(cubierto), new BigDecimal(cubierto)));
        when(ejecucion.nuevo(any(), any(), any())).thenReturn(new Cobro());
        return inscripcion;
    }

    @Test
    void registrar_inscripcionInexistente_404() {
        when(inscripcionRepository.conActividad(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registrar_inscripcionDeOtroSocio_404() {
        inscripcion(EstadoInscripcion.PENDIENTE_PAGO, true, "10000.00", "0.00");

        assertThatThrownBy(() -> cobroDeActividad.registrar(9L, QR, new Actor(99L, Rol.SOCIO))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registrar_inscripcionCancelada_422() {
        inscripcion(EstadoInscripcion.CANCELADA, false, "10000.00", "0.00");

        assertThatThrownBy(() -> cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("INSCRIPCION_NO_COBRABLE");
    }

    @Test
    void registrar_pendiente_cobraLaSenia() {
        inscripcion(EstadoInscripcion.PENDIENTE_PAGO, true, "10000.00", "0.00");

        cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO);

        verify(ejecucion).nuevo(MedioPago.EFECTIVO, ConceptoCobro.SEÑA, EMPLEADO);
        verify(ejecucion).asentar(any(), eq(new BigDecimal("5000.00")), eq(3L), eq(EFECTIVO), any());
    }

    @Test
    void registrar_confirmada_cobraLoQueFaltaDelTotal() {
        inscripcion(EstadoInscripcion.CONFIRMADA, true, "10000.00", "5000.00");

        cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO);

        verify(ejecucion).nuevo(MedioPago.EFECTIVO, ConceptoCobro.SALDO, EMPLEADO);
        verify(ejecucion).asentar(any(), eq(new BigDecimal("5000.00")), eq(3L), eq(EFECTIVO), any());
    }

    @Test
    void registrar_sinSaldoPendiente_409() {
        inscripcion(EstadoInscripcion.CONFIRMADA, true, "10000.00", "10000.00");

        assertThatThrownBy(() -> cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("COBRO_YA_REGISTRADO");
    }

    @Test
    void registrar_sinMontoTotal_409NoCobraNada() {
        inscripcion(EstadoInscripcion.CONFIRMADA, true, null, "0.00");

        assertThatThrownBy(() -> cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void registrar_conQrEnCurso_reutilizaElCobro() {
        inscripcion(EstadoInscripcion.PENDIENTE_PAGO, true, "10000.00", "0.00");
        Cobro enCurso = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "5000.00");
        when(cobroRepository.deInscripcionActividad(eq(9L), eq(List.of(EstadoCobro.PENDIENTE)), any())).thenReturn(List.of(enCurso));

        cobroDeActividad.registrar(9L, QR, SOCIO);

        verify(cobroMapper).aRespuesta(enCurso);
        verify(ejecucion, never()).asentar(any(), any(), any(), any(), any());
    }

    @Test
    void registrar_conQrRechazado_cierraSuLinkAntesDeCobrar() {
        inscripcion(EstadoInscripcion.PENDIENTE_PAGO, true, "10000.00", "0.00");
        List<Cobro> rechazados = List.of(cobro(7L, EstadoCobro.RECHAZADO, MedioPago.QR, "5000.00"));
        when(cobroRepository.deInscripcionActividad(eq(9L), eq(List.of(EstadoCobro.RECHAZADO)), any())).thenReturn(rechazados);

        cobroDeActividad.registrar(9L, EFECTIVO, EMPLEADO);

        verify(reemplazoDeQr).reemplazarRechazados(rechazados, EMPLEADO);
    }
}
