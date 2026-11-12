package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.mercadopago.EstadoDeOrden;
import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException;
import com.tenisahora.infrastructure.mercadopago.ResultadoDePago;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.service.ConfirmacionDeInscripcion;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcreditacionBordesTest {

    private static final BigDecimal MONTO = new BigDecimal("8000.00");

    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private PasarelaDePagos pasarela;
    @Mock
    private EmisionDeRecibos emisionDeRecibos;
    @Mock
    private ConfirmacionDeReserva confirmacionDeReserva;
    @Mock
    private CierreDeOrden cierreDeOrden;
    @Mock
    private ConfirmacionDeInscripcion confirmacionDeInscripcion;
    @Mock
    private ConfirmacionDeInscripcionTorneo confirmacionDeTorneo;
    @InjectMocks
    private AcreditacionDePagosImpl acreditacion;

    private Cobro enLaOrden(EstadoCobro estado) {
        Cobro cobro = cobro(7L, estado, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroRepository.buscarPorPagoExternoParaModificar("orden-1")).thenReturn(Optional.of(cobro));
        return cobro;
    }

    private void laPasarelaInforma(ResultadoDePago resultado, BigDecimal monto) {
        when(pasarela.consultar("orden-1")).thenReturn(new EstadoDeOrden("orden-1", resultado, monto, "186778732035"));
    }

    @Test
    void acreditar_unPagoAprobadoConElQrYaVencido_igualSeRegistra() {
        Cobro cobro = enLaOrden(EstadoCobro.VENCIDO);
        laPasarelaInforma(ResultadoDePago.ACREDITADO, MONTO);

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
        verify(emisionDeRecibos).emitir(cobro);
    }

    @ParameterizedTest
    @EnumSource(value = ResultadoDePago.class, names = {"PENDIENTE", "RECHAZADO", "VENCIDO"})
    void acreditar_unCobroVencidoSinPagoAprobado_sigueVencido(ResultadoDePago resultado) {
        Cobro cobro = enLaOrden(EstadoCobro.VENCIDO);
        laPasarelaInforma(resultado, BigDecimal.ZERO);

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.VENCIDO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCobro.class, names = {"PAGADO", "ANULADO"})
    void acreditar_unCobroFinal_noConsultaNiCambiaNada(EstadoCobro estado) {
        Cobro cobro = enLaOrden(estado);

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(estado);
        verifyNoInteractions(pasarela, emisionDeRecibos, confirmacionDeReserva);
    }

    @Test
    void acreditar_laPasarelaInformaVencido_quedaVencidoYSinRecibo() {
        Cobro cobro = enLaOrden(EstadoCobro.PENDIENTE);
        laPasarelaInforma(ResultadoDePago.VENCIDO, BigDecimal.ZERO);

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.VENCIDO);
        verify(emisionDeRecibos, never()).emitir(any());
    }

    @Test
    void acreditar_rechazadoQueSigueRechazado_noEmiteRecibo() {
        Cobro cobro = enLaOrden(EstadoCobro.RECHAZADO);
        laPasarelaInforma(ResultadoDePago.RECHAZADO, BigDecimal.ZERO);

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.RECHAZADO);
        verify(emisionDeRecibos, never()).emitir(any());
    }

    @Test
    void acreditar_unPagoPorMenosOMas_noRegistraNada() {
        Cobro cobro = enLaOrden(EstadoCobro.PENDIENTE);
        laPasarelaInforma(ResultadoDePago.ACREDITADO, new BigDecimal("7999.99"));
        acreditacion.acreditar("orden-1");
        laPasarelaInforma(ResultadoDePago.ACREDITADO, new BigDecimal("8000.01"));
        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PENDIENTE);
        verify(emisionDeRecibos, never()).emitir(any());
    }

    @Test
    void acreditar_elMismoMontoConOtraEscala_loAcepta() {
        Cobro cobro = enLaOrden(EstadoCobro.PENDIENTE);
        laPasarelaInforma(ResultadoDePago.ACREDITADO, new BigDecimal("8000"));

        acreditacion.acreditar("orden-1");

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
    }

    @Test
    void acreditar_laPasarelaCaida_propagaElErrorSinCambiarElCobro() {
        Cobro cobro = enLaOrden(EstadoCobro.PENDIENTE);
        when(pasarela.consultar("orden-1")).thenThrow(new PasarelaNoDisponibleException("no responde"));

        assertThatThrownBy(() -> acreditacion.acreditar("orden-1")).isInstanceOf(PasarelaNoDisponibleException.class);

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PENDIENTE);
    }

    @Test
    void acreditar_unaActividad_confirmaLaInscripcion() {
        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setId(9L);
        Cobro cobro = enLaOrden(EstadoCobro.PENDIENTE);
        cobro.setReserva(null);
        cobro.setInscripcionActividad(inscripcion);
        laPasarelaInforma(ResultadoDePago.ACREDITADO, MONTO);
        when(confirmacionDeInscripcion.confirmarPagoAcreditado(9L, MONTO)).thenReturn(true);

        acreditacion.acreditar("orden-1");

        verify(confirmacionDeInscripcion).confirmarPagoAcreditado(9L, MONTO);
    }

    @Test
    void acreditar_unTorneoYaCancelado_registraElPagoYNoFalla() {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(9L);
        Cobro cobro = enLaOrden(EstadoCobro.PENDIENTE);
        cobro.setReserva(null);
        cobro.setInscripcionTorneo(inscripcion);
        laPasarelaInforma(ResultadoDePago.ACREDITADO, MONTO);
        when(confirmacionDeTorneo.confirmarPagoAcreditado(9L)).thenReturn(false);

        assertThatCode(() -> acreditacion.acreditar("orden-1")).doesNotThrowAnyException();

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
        verify(cierreDeOrden).cerrar("orden-1");
    }
}
