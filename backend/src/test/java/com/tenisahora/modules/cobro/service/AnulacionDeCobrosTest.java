package com.tenisahora.modules.cobro.service;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.actividad.service.ConfirmacionDeInscripcion;
import com.tenisahora.modules.cobro.dto.CobroResponse;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.service.ConfirmacionDeInscripcionTorneo;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.EstadoRecibo;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.modules.reserva.service.ConfirmacionDeReserva;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.RELOJ;
import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnulacionDeCobrosTest {

    private static final Actor ADMIN = new Actor(5L, Rol.ADMINISTRADOR);

    @Mock
    private CobroPorId cobroPorId;
    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private ReciboRepository reciboRepository;
    @Mock
    private CierreDeOrden cierreDeOrden;
    @Mock
    private ConfirmacionDeReserva confirmacionDeReserva;
    @Mock
    private ConfirmacionDeInscripcion confirmacionDeInscripcion;
    @Mock
    private ConfirmacionDeInscripcionTorneo confirmacionDeTorneo;

    private AnulacionDeCobros anulacion;

    @BeforeEach
    void setUp() {
        anulacion = new AnulacionDeCobros(cobroPorId, new RegistroDeAnulacion(cobroRepository, reciboRepository, RELOJ),
                cierreDeOrden, confirmacionDeReserva, new CobroMapper(), confirmacionDeInscripcion, confirmacionDeTorneo, cobroRepository);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCobro.class, names = {"VENCIDO", "RECHAZADO", "ANULADO"})
    void anular_unEstadoQueNoSeAnula_422(EstadoCobro estado) {
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro(7L, estado, MedioPago.QR, "8000.00"));

        assertThatThrownBy(() -> anulacion.anular(7L, "Motivo", ADMIN))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("COBRO_NO_ANULABLE");
    }

    @Test
    void anular_pendienteConQr_cierraElLinkYNoTocaLaReserva() {
        Cobro cobro = cobro(7L, EstadoCobro.PENDIENTE, MedioPago.QR, "8000.00");
        cobro.setIdPagoExterno("orden-1");
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);

        anulacion.anular(7L, "Motivo", ADMIN);

        verify(cierreDeOrden).cerrar("orden-1");
        verify(confirmacionDeReserva, never()).volverAPendiente(any());
    }

    @Test
    void anular_pagadoConTarjeta_noHayLinkQueCerrar() {
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00"));

        anulacion.anular(7L, "Motivo", ADMIN);

        verify(cierreDeOrden, never()).cerrar(any());
    }

    @Test
    void anular_seniaConSaldoPagado_422YNoAnulaNada() {
        Cobro senia = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        Cobro saldo = cobro(8L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        saldo.setConcepto(ConceptoCobro.SALDO);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(senia);
        when(cobroRepository.findByReservaIdAndEstado(senia.getReserva().getId(), EstadoCobro.PAGADO)).thenReturn(List.of(senia, saldo));

        assertThatThrownBy(() -> anulacion.anular(7L, "Motivo", ADMIN)).isInstanceOf(ReglaDeNegocioException.class);

        assertThat(senia.getEstado()).isEqualTo(EstadoCobro.PAGADO);
        verify(confirmacionDeReserva, never()).volverAPendiente(any());
    }

    @Test
    void anular_seniaDeActividadConSaldoPagado_422() {
        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setId(9L);
        Cobro senia = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        senia.setReserva(null);
        senia.setInscripcionActividad(inscripcion);
        Cobro saldo = cobro(8L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        saldo.setConcepto(ConceptoCobro.SALDO);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(senia);
        when(cobroRepository.deInscripcionActividad(any(), any(), any())).thenReturn(List.of(saldo));

        assertThatThrownBy(() -> anulacion.anular(7L, "Motivo", ADMIN)).isInstanceOf(ReglaDeNegocioException.class);

        verify(confirmacionDeInscripcion, never()).volverAPendiente(any());
    }

    @Test
    void anular_seniaDeActividadPagada_laInscripcionVuelveAPendiente() {
        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setId(9L);
        inscripcion.setActividad(mock(Actividad.class));
        Cobro senia = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        senia.setReserva(null);
        senia.setInscripcionActividad(inscripcion);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(senia);

        anulacion.anular(7L, "Motivo", ADMIN);

        verify(confirmacionDeInscripcion).volverAPendiente(9L);
    }

    @Test
    void anular_saldoDeActividad_noTocaLaInscripcion() {
        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setId(9L);
        inscripcion.setActividad(mock(Actividad.class));
        Cobro saldo = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        saldo.setReserva(null);
        saldo.setConcepto(ConceptoCobro.SALDO);
        saldo.setInscripcionActividad(inscripcion);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(saldo);

        anulacion.anular(7L, "Motivo", ADMIN);

        verify(confirmacionDeInscripcion, never()).volverAPendiente(any());
    }

    @Test
    void anular_arancelDeTorneoPagado_laInscripcionVuelveAPendiente() {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(9L);
        Cobro arancel = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        arancel.setReserva(null);
        arancel.setConcepto(ConceptoCobro.INSCRIPCION);
        arancel.setInscripcionTorneo(inscripcion);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(arancel);

        anulacion.anular(7L, "Motivo", ADMIN);

        verify(confirmacionDeTorneo).volverAPendiente(9L);
    }

    @Test
    void anular_reembolso_noTocaLaReserva() {
        Cobro reembolso = cobro(7L, EstadoCobro.PAGADO, MedioPago.EFECTIVO, "8000.00");
        reembolso.setConcepto(ConceptoCobro.REEMBOLSO);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(reembolso);

        anulacion.anular(7L, "Motivo", ADMIN);

        verify(confirmacionDeReserva, never()).volverAPendiente(any());
    }

    @Test
    void anular_seniaPagada_dejaMotivoFechaYResponsable_anulaElReciboYLaReservaVuelveAPendiente() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        Recibo recibo = new Recibo();
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);
        when(reciboRepository.findByCobroId(7L)).thenReturn(Optional.of(recibo));

        CobroResponse respuesta = anulacion.anular(7L, "Se cargó mal", ADMIN);

        assertThat(respuesta.estado()).isEqualTo(EstadoCobro.ANULADO);
        assertThat(respuesta.motivoAnulacion()).isEqualTo("Se cargó mal");
        assertThat(respuesta.fechaAnulacion()).isNotNull();
        assertThat(respuesta.anuladoPorId()).isEqualTo(5L);
        assertThat(recibo.getEstado()).isEqualTo(EstadoRecibo.ANULADO);
        verify(confirmacionDeReserva).volverAPendiente(cobro.getReserva().getId());
    }

    @Test
    void anular_saldoPagado_noTocaLaReserva() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.DEBITO, "8000.00");
        cobro.setConcepto(ConceptoCobro.SALDO);
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro);

        anulacion.anular(7L, "Se cargó mal", ADMIN);

        verify(confirmacionDeReserva, never()).volverAPendiente(any());
    }

    @Test
    void anular_unoYaAnulado_422() {
        when(cobroPorId.buscarParaModificar(7L)).thenReturn(cobro(7L, EstadoCobro.ANULADO, MedioPago.DEBITO, "8000.00"));

        assertThatThrownBy(() -> anulacion.anular(7L, "Otra vez", ADMIN)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
