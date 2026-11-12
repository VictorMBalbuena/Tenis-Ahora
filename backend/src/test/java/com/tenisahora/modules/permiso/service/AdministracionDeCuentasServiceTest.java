package com.tenisahora.modules.permiso.service;

import com.tenisahora.modules.permiso.dto.CambioDeEstadoRequest;
import com.tenisahora.modules.permiso.dto.CambioDeRolRequest;
import com.tenisahora.modules.permiso.entity.MovimientoDeCuenta;
import com.tenisahora.modules.permiso.entity.RolDelSistema;
import com.tenisahora.modules.permiso.repository.MovimientoDeCuentaRepository;
import com.tenisahora.shared.cuenta.DatosDeCuenta;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.event.CuentaDeshabilitadaEvent;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ErrorApiException;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CU74 (asignar rol) y CU76 (habilitar y deshabilitar cuentas). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdministracionDeCuentasServiceTest {

    private static final Actor ADMIN = new Actor(1L, Rol.ADMINISTRADOR);
    private static final Actor EMPLEADO = new Actor(2L, Rol.EMPLEADO);

    @Mock
    private DirectorioDeCuentas directorio;
    @Mock
    private RolService roles;
    @Mock
    private MovimientoDeCuentaRepository movimientos;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private AdministracionDeCuentasService servicio;

    private DatosDeCuenta cuenta(Long id, Rol rol, boolean activa) {
        return new DatosDeCuenta(id, "Ana", "Gómez", "30123456", "ana@correo.com", rol, activa, null);
    }

    private void existe(DatosDeCuenta cuenta) {
        when(directorio.exigir(cuenta.id())).thenReturn(cuenta);
        when(roles.nombreDe(any())).thenReturn(Optional.empty());
    }

    // ---------- CU74 ----------

    @Test
    void cambiarDeRolBase_mudaLaCuentaYDevuelveElIdNuevo() {
        existe(cuenta(5L, Rol.SOCIO, true));
        when(directorio.traspasar(5L, Rol.SOCIO, Rol.EMPLEADO)).thenReturn(30L);
        when(directorio.exigir(30L)).thenReturn(cuenta(30L, Rol.EMPLEADO, true));

        var respuesta = servicio.asignarRol(5L, new CambioDeRolRequest(Rol.EMPLEADO, null, "Lo contratan"), ADMIN);

        assertThat(respuesta.id()).isEqualTo(30L);
        assertThat(respuesta.rol()).isEqualTo(Rol.EMPLEADO);
        verify(directorio).asignarRolPersonalizado(30L, Rol.EMPLEADO, null);
    }

    @Test
    void cambiarDeRol_dejaElHistorialPegadoAlIdNuevo() {
        existe(cuenta(5L, Rol.SOCIO, true));
        when(directorio.traspasar(5L, Rol.SOCIO, Rol.EMPLEADO)).thenReturn(30L);
        when(directorio.exigir(30L)).thenReturn(cuenta(30L, Rol.EMPLEADO, true));

        servicio.asignarRol(5L, new CambioDeRolRequest(Rol.EMPLEADO, null, null), ADMIN);

        verify(movimientos).trasladarHistorial(5L, 30L);
    }

    @Test
    void mismoRolBase_soloCambiaElRolPersonalizadoSinMudarLaCuenta() {
        existe(cuenta(5L, Rol.EMPLEADO, true));
        RolDelSistema recepcion = new RolDelSistema();
        recepcion.setId(9L);
        when(roles.personalizadoCompatible(9L, Rol.EMPLEADO)).thenReturn(recepcion);

        servicio.asignarRol(5L, new CambioDeRolRequest(Rol.EMPLEADO, 9L, null), ADMIN);

        verify(directorio, never()).traspasar(any(), any(), any());
        verify(directorio).asignarRolPersonalizado(5L, Rol.EMPLEADO, 9L);
    }

    @Test
    void soloUnAdministradorPuedeAsignarElRolAdministrador() {
        existe(cuenta(5L, Rol.SOCIO, true));

        assertThatThrownBy(() -> servicio.asignarRol(5L,
                new CambioDeRolRequest(Rol.ADMINISTRADOR, null, null), EMPLEADO))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.SOLO_UN_ADMINISTRADOR_ASIGNA_ADMINISTRADORES);
    }

    @Test
    void nadieSeCambiaElRolASiMismo() {
        existe(cuenta(1L, Rol.ADMINISTRADOR, true));

        assertThatThrownBy(() -> servicio.asignarRol(1L, new CambioDeRolRequest(Rol.EMPLEADO, null, null), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.CUENTA_PROPIA_NO_ADMINISTRABLE);
    }

    @Test
    void elUltimoAdministradorNoPuedeDejarDeSerlo() {
        existe(cuenta(7L, Rol.ADMINISTRADOR, true));
        when(directorio.activasCon(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.asignarRol(7L, new CambioDeRolRequest(Rol.EMPLEADO, null, null), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.PERSONA_ULTIMO_ADMINISTRADOR);
    }

    @Test
    void asignarElMismoRolQueYaTiene_noHaceNada() {
        existe(cuenta(5L, Rol.SOCIO, true));

        assertThatThrownBy(() -> servicio.asignarRol(5L, new CambioDeRolRequest(Rol.SOCIO, null, null), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.ROL_SIN_CAMBIOS);
    }

    @Test
    void elCambioDeRolQuedaRegistradoConQuienLoHizo() {
        existe(cuenta(5L, Rol.SOCIO, true));
        when(directorio.traspasar(5L, Rol.SOCIO, Rol.EMPLEADO)).thenReturn(30L);
        when(directorio.exigir(30L)).thenReturn(cuenta(30L, Rol.EMPLEADO, true));

        servicio.asignarRol(5L, new CambioDeRolRequest(Rol.EMPLEADO, null, "Lo contratan"), ADMIN);

        ArgumentCaptor<MovimientoDeCuenta> captor = ArgumentCaptor.forClass(MovimientoDeCuenta.class);
        verify(movimientos).save(captor.capture());
        MovimientoDeCuenta movimiento = captor.getValue();
        assertThat(movimiento.getTipo()).isEqualTo(MovimientoDeCuenta.Tipo.CAMBIO_DE_ROL);
        assertThat(movimiento.getDetalle()).isEqualTo("Pasó de Socio a Empleado");
        assertThat(movimiento.getMotivo()).isEqualTo("Lo contratan");
        assertThat(movimiento.getHechoPorId()).isEqualTo(ADMIN.id());
        assertThat(movimiento.getPersonaIdAnterior()).isEqualTo(5L);
    }

    // ---------- CU76 ----------

    @Test
    void deshabilitarSinMotivo_seRechaza() {
        existe(cuenta(5L, Rol.SOCIO, true));

        assertThatThrownBy(() -> servicio.cambiarEstado(5L, new CambioDeEstadoRequest(false, "  ", false), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.CUENTA_MOTIVO_REQUERIDO);
    }

    @Test
    void deshabilitar_cambiaElEstadoYAvisaParaQueSeCierrenLasSesiones() {
        existe(cuenta(5L, Rol.SOCIO, true));

        servicio.cambiarEstado(5L, new CambioDeEstadoRequest(false, "Dejó el club", true), ADMIN);

        verify(directorio).cambiarEstado(5L, Rol.SOCIO, false);
        ArgumentCaptor<CuentaDeshabilitadaEvent> captor = ArgumentCaptor.forClass(CuentaDeshabilitadaEvent.class);
        verify(eventos).publishEvent(captor.capture());
        assertThat(captor.getValue().personaId()).isEqualTo(5L);
        assertThat(captor.getValue().cancelarReservasFuturas()).isTrue();
    }

    @Test
    void habilitar_noAvisaNadaYNoPideMotivo() {
        existe(cuenta(5L, Rol.SOCIO, false));

        servicio.cambiarEstado(5L, new CambioDeEstadoRequest(true, null, false), ADMIN);

        verify(directorio).cambiarEstado(5L, Rol.SOCIO, true);
        verify(eventos, never()).publishEvent(any(CuentaDeshabilitadaEvent.class));
    }

    @Test
    void laCuentaYaEstaEnEseEstado_seRechaza() {
        existe(cuenta(5L, Rol.SOCIO, true));

        assertThatThrownBy(() -> servicio.cambiarEstado(5L, new CambioDeEstadoRequest(true, null, false), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.CUENTA_YA_EN_ESE_ESTADO);
    }

    @Test
    void nadieSeDeshabilitaASiMismo() {
        existe(cuenta(1L, Rol.ADMINISTRADOR, true));

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, new CambioDeEstadoRequest(false, "x", false), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.CUENTA_PROPIA_NO_ADMINISTRABLE);
    }

    @Test
    void elUltimoAdministradorNoSePuedeDeshabilitar() {
        existe(cuenta(7L, Rol.ADMINISTRADOR, true));
        when(directorio.activasCon(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarEstado(7L, new CambioDeEstadoRequest(false, "x", false), ADMIN))
                .isInstanceOf(ErrorApiException.class)
                .extracting(e -> ((ErrorApiException) e).getCodigoError())
                .isEqualTo(CodigoError.PERSONA_ULTIMO_ADMINISTRADOR);
    }

    @Test
    void laBajaQuedaRegistradaConElMotivo() {
        existe(cuenta(5L, Rol.SOCIO, true));

        servicio.cambiarEstado(5L, new CambioDeEstadoRequest(false, "Dejó el club", true), ADMIN);

        ArgumentCaptor<MovimientoDeCuenta> captor = ArgumentCaptor.forClass(MovimientoDeCuenta.class);
        verify(movimientos).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(MovimientoDeCuenta.Tipo.DESHABILITACION);
        assertThat(captor.getValue().getDetalle()).contains("se cancelaron sus reservas futuras");
        assertThat(captor.getValue().getMotivo()).isEqualTo("Dejó el club");
    }
}
