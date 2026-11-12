package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.mercadopago.PasarelaDePagos;
import com.tenisahora.infrastructure.mercadopago.PasarelaNoDisponibleException;
import com.tenisahora.modules.cobro.DatosDePrueba;
import com.tenisahora.modules.cobro.dto.CobroRequest;
import com.tenisahora.modules.cobro.entity.AvisoDePagoDescartado;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.ConceptoCobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.AvisoDePagoDescartadoRepository;
import com.tenisahora.modules.cobro.repository.CobroRepository;
import com.tenisahora.modules.cobro.service.pago.FormasDeCobro;
import com.tenisahora.modules.cobro.service.pago.MedioDePago;
import com.tenisahora.modules.cobro.service.pago.ResultadoDeCobro;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvisosYAsentadoBordesTest {

    @Mock
    private PasarelaDePagos pasarela;
    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private AcreditacionDePagos acreditacionDePagos;
    @Mock
    private AvisoDePagoDescartadoRepository avisoRepository;
    @Mock
    private CalculadoraDeCobro calculadoraDeCobro;
    @Mock
    private EmisionDeRecibos emisionDeRecibos;

    private AvisoDePagoDescartado guardado() {
        ArgumentCaptor<AvisoDePagoDescartado> aviso = ArgumentCaptor.forClass(AvisoDePagoDescartado.class);
        verify(avisoRepository).save(aviso.capture());
        return aviso.getValue();
    }

    @Test
    void registrarAviso_sinDatos_igualQuedaConLaFecha() {
        new RegistroDeAvisosDescartados(avisoRepository, DatosDePrueba.RELOJ).registrar(null, null);

        assertThat(guardado().getFechaRecepcion()).isEqualTo(DatosDePrueba.AHORA);
    }

    @Test
    void registrarAviso_conDatosEnElLimite_losGuardaEnteros() {
        new RegistroDeAvisosDescartados(avisoRepository, DatosDePrueba.RELOJ).registrar("1".repeat(60), "2".repeat(100));

        assertThat(guardado().getIdPago()).hasSize(60);
    }

    @Test
    void registrarAviso_conDatosMasLargos_losRecorta() {
        new RegistroDeAvisosDescartados(avisoRepository, DatosDePrueba.RELOJ).registrar("1".repeat(61), "2".repeat(5000));

        AvisoDePagoDescartado aviso = guardado();
        assertThat(aviso.getIdPago()).hasSize(60);
        assertThat(aviso.getIdPedido()).hasSize(100);
    }

    private AcreditacionAsincronica asincronica() {
        return new AcreditacionAsincronica(pasarela, cobroRepository, acreditacionDePagos);
    }

    @Test
    void avisoAsincronico_unPagoDeOtroSistema_noAcreditaNada() {
        when(pasarela.referenciaExternaDelPago("1")).thenReturn("otra-referencia");
        when(cobroRepository.findByCodigo("otra-referencia")).thenReturn(Optional.empty());

        asincronica().procesarAvisoDePago("1");

        verifyNoInteractions(acreditacionDePagos);
    }

    @Test
    void avisoAsincronico_unCobroSinOrden_noAcreditaNada() {
        when(pasarela.referenciaExternaDelPago("1")).thenReturn("C-000007");
        when(cobroRepository.findByCodigo("C-000007")).thenReturn(Optional.of(DatosDePrueba.cobro(7L, EstadoCobro.PENDIENTE, MedioPago.DEBITO, "1.00")));

        asincronica().procesarAvisoDePago("1");

        verifyNoInteractions(acreditacionDePagos);
    }

    @Test
    void avisoAsincronico_conLaPasarelaCaidaOUnIdQueNoEsNumero_noRompeElHilo() {
        when(pasarela.referenciaExternaDelPago("1")).thenThrow(new PasarelaNoDisponibleException("no responde"));
        when(pasarela.referenciaExternaDelPago("abc")).thenThrow(new NumberFormatException("abc"));

        assertThatCode(() -> {
            asincronica().procesarAvisoDePago("1");
            asincronica().procesarAvisoDePago("abc");
        }).doesNotThrowAnyException();
    }

    private EjecucionDeCobro ejecucion(ResultadoDeCobro resultado) {
        MedioDePago medio = mock(MedioDePago.class);
        when(medio.medio()).thenReturn(MedioPago.DEBITO);
        when(medio.cobrar(any(), any())).thenReturn(resultado);
        when(calculadoraDeCobro.aplicarDescuentos(any(), any(), any()))
                .thenReturn(new CalculadoraDeCobro.MontoDeCobro(new BigDecimal("100.00"), new BigDecimal("90.00"), List.of()));
        when(cobroRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Cobro cobro = inv.getArgument(0);
            cobro.setId(7L);
            return cobro;
        });
        return new EjecucionDeCobro(cobroRepository, calculadoraDeCobro, new FormasDeCobro(List.of(medio)), emisionDeRecibos, new CobroMapper());
    }

    @Test
    void asentar_pagado_emiteElReciboYAvisaUnaVez() {
        Runnable alQuedarPago = mock(Runnable.class);
        EjecucionDeCobro ejecucion = ejecucion(ResultadoDeCobro.pagado("1"));
        Cobro cobro = ejecucion.nuevo(MedioPago.DEBITO, ConceptoCobro.INSCRIPCION, new Actor(1L, Rol.EMPLEADO));

        ejecucion.asentar(cobro, new BigDecimal("100.00"), 3L, new CobroRequest(MedioPago.DEBITO, "1", null, true), alQuedarPago);

        assertThat(cobro.getMontoFinal()).isEqualByComparingTo("90.00");
        verify(emisionDeRecibos).emitir(cobro);
        verify(alQuedarPago).run();
    }

    @Test
    void asentar_rechazado_noEmiteReciboNiConfirma() {
        Runnable alQuedarPago = mock(Runnable.class);
        EjecucionDeCobro ejecucion = ejecucion(ResultadoDeCobro.rechazado("1"));
        Cobro cobro = ejecucion.nuevo(MedioPago.DEBITO, ConceptoCobro.INSCRIPCION, new Actor(1L, Rol.EMPLEADO));

        ejecucion.asentar(cobro, new BigDecimal("100.00"), 3L, new CobroRequest(MedioPago.DEBITO, "1", null, false), alQuedarPago);

        assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.RECHAZADO);
        verify(emisionDeRecibos, never()).emitir(any());
        verify(alQuedarPago, never()).run();
    }

    @Test
    void nuevo_elSocioNoQuedaComoQuienCobro() {
        EjecucionDeCobro ejecucion = ejecucion(ResultadoDeCobro.pagado("1"));

        assertThat(ejecucion.nuevo(MedioPago.QR, ConceptoCobro.SEÑA, new Actor(3L, Rol.SOCIO)).getRegistradoPorId()).isNull();
        assertThat(ejecucion.nuevo(MedioPago.DEBITO, ConceptoCobro.SEÑA, new Actor(1L, Rol.EMPLEADO)).getRegistradoPorId()).isEqualTo(1L);
    }
}
