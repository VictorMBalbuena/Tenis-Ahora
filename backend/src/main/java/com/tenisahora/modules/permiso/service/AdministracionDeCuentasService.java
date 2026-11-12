package com.tenisahora.modules.permiso.service;

import com.tenisahora.modules.permiso.dto.CambioDeEstadoRequest;
import com.tenisahora.modules.permiso.dto.CambioDeRolRequest;
import com.tenisahora.modules.permiso.dto.CuentaResponse;
import com.tenisahora.modules.permiso.dto.MovimientoResponse;
import com.tenisahora.modules.permiso.entity.MovimientoDeCuenta;
import com.tenisahora.modules.permiso.repository.MovimientoDeCuentaRepository;
import com.tenisahora.shared.cuenta.DatosDeCuenta;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.event.CuentaDeshabilitadaEvent;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Asignación de rol (CU74 / RF74) y habilitación o deshabilitación de cuentas (CU76 / RF76).
 *
 * <p>Las dos operaciones trabajan sobre cualquiera de los cinco roles, así que pasan por
 * {@link DirectorioDeCuentas} y no conocen ninguna entidad de rol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdministracionDeCuentasService {

    private final DirectorioDeCuentas directorio;
    private final RolService roles;
    private final MovimientoDeCuentaRepository movimientos;
    private final ApplicationEventPublisher eventos;

    @Transactional(readOnly = true)
    public CuentaResponse consultar(Long id) {
        return aRespuesta(directorio.exigir(id));
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> historial(Long id) {
        return movimientos.historialDe(id).stream().map(MovimientoResponse::de).toList();
    }

    // ---------- CU74: asignar rol ----------

    /**
     * Le cambia el rol a una persona. Si el rol base es el mismo, solo cambia el rol personalizado; si cambia el
     * rol base, la cuenta se muda a la tabla del rol nuevo conservando sus datos y su contraseña.
     *
     * <p>Ese traspaso le cambia el id a la persona, porque cada rol es una tabla distinta. Solo puede hacerse si
     * la cuenta no dejó movimientos con su rol actual: si los tiene, el traspaso falla con PERSONA_CON_HISTORIAL
     * y no se pierde nada.
     */
    @Transactional
    public CuentaResponse asignarRol(Long id, CambioDeRolRequest request, Actor actor) {
        DatosDeCuenta cuenta = directorio.exigir(id);
        Rol rolNuevo = request.rol();

        Long rolPersonalizadoId = validarRolPersonalizado(request.rolPersonalizadoId(), rolNuevo);
        asegurarQueSePuedeAsignarElRol(cuenta, rolNuevo, rolPersonalizadoId, actor);

        Long idFinal = mudarSiCambiaDeTabla(id, cuenta.rol(), rolNuevo);
        directorio.asignarRolPersonalizado(idFinal, rolNuevo, rolPersonalizadoId);

        registrar(cuenta, idFinal, MovimientoDeCuenta.Tipo.CAMBIO_DE_ROL, descripcionDelCambio(cuenta, rolNuevo,
                rolPersonalizadoId), request.motivo(), actor);
        log.info("Persona {} pasó de {} a {} por decisión de la persona {}", id, cuenta.rol(), rolNuevo, actor.id());

        return aRespuesta(directorio.exigir(idFinal));
    }

    /**
     * Si cambia el rol base, la fila se muda a la tabla del rol nuevo y la persona queda con otro id. El traspaso
     * valida solo que no haya historial: si lo hay, falla antes de borrar nada.
     */
    private Long mudarSiCambiaDeTabla(Long id, Rol rolActual, Rol rolNuevo) {
        if (rolActual == rolNuevo) {
            return id;
        }
        Long idFinal = directorio.traspasar(id, rolActual, rolNuevo);
        // El id cambió, pero lo que ya se le había hecho a esta cuenta sigue siendo suyo
        movimientos.trasladarHistorial(id, idFinal);
        return idFinal;
    }

    private void asegurarQueSePuedeAsignarElRol(DatosDeCuenta cuenta, Rol rolNuevo, Long rolPersonalizadoId,
                                                Actor actor) {
        asegurarQueNoEsSuPropiaCuenta(cuenta, actor, "cambiarte el rol a vos mismo");
        asegurarQuePuedeAsignar(rolNuevo, actor);
        asegurarQueNoEsElUltimoAdministrador(cuenta, rolNuevo != Rol.ADMINISTRADOR);
        asegurarQueAlgoCambia(cuenta, rolNuevo, rolPersonalizadoId);
    }

    // ---------- CU76: habilitar y deshabilitar ----------

    /**
     * Deshabilita una cuenta sin borrar nada, o la vuelve a habilitar. Al deshabilitar, el motivo es obligatorio,
     * se le cierran las sesiones abiertas y, si el administrador lo pidió, se le cancelan los turnos futuros.
     */
    @Transactional
    public CuentaResponse cambiarEstado(Long id, CambioDeEstadoRequest request, Actor actor) {
        DatosDeCuenta cuenta = directorio.exigir(id);
        boolean activa = Boolean.TRUE.equals(request.activa());
        asegurarQueSePuedeCambiarElEstado(cuenta, activa, request, actor);

        directorio.cambiarEstado(id, cuenta.rol(), activa);
        if (!activa) {
            // auth cierra las sesiones y reservas cancela los turnos futuros, cada uno por su cuenta
            eventos.publishEvent(new CuentaDeshabilitadaEvent(id,
                    Boolean.TRUE.equals(request.cancelarReservasFuturas()), request.motivo()));
        }

        registrar(cuenta, id, activa ? MovimientoDeCuenta.Tipo.HABILITACION : MovimientoDeCuenta.Tipo.DESHABILITACION,
                activa ? "Se habilitó la cuenta" : descripcionDeLaBaja(request), request.motivo(), actor);
        log.info("Cuenta {} {} por la persona {}", id, activa ? "habilitada" : "deshabilitada", actor.id());

        return aRespuesta(directorio.exigir(id));
    }

    private void asegurarQueSePuedeCambiarElEstado(DatosDeCuenta cuenta, boolean activa,
                                                   CambioDeEstadoRequest request, Actor actor) {
        asegurarQueNoEsSuPropiaCuenta(cuenta, actor, "deshabilitar tu propia cuenta");
        if (cuenta.activa() == activa) {
            throw new ReglaDeNegocioException(CodigoError.CUENTA_YA_EN_ESE_ESTADO,
                    activa ? "La cuenta ya está habilitada" : "La cuenta ya está deshabilitada");
        }
        if (!activa) {
            exigirMotivo(request.motivo());
            asegurarQueNoEsElUltimoAdministrador(cuenta, true);
        }
    }

    // ---------- Reglas ----------

    private void asegurarQueNoEsSuPropiaCuenta(DatosDeCuenta cuenta, Actor actor, String accion) {
        if (cuenta.id().equals(actor.id())) {
            throw new ReglaDeNegocioException(CodigoError.CUENTA_PROPIA_NO_ADMINISTRABLE,
                    "No podés " + accion);
        }
    }

    /** RF74: el rol Administrador solo lo asigna un administrador. */
    private void asegurarQuePuedeAsignar(Rol rolNuevo, Actor actor) {
        if (rolNuevo == Rol.ADMINISTRADOR && !actor.esAdministrador()) {
            throw new ReglaDeNegocioException(CodigoError.SOLO_UN_ADMINISTRADOR_ASIGNA_ADMINISTRADORES,
                    "Solo un administrador puede asignar el rol Administrador");
        }
    }

    /** El club no puede quedarse sin ningún administrador activo. */
    private void asegurarQueNoEsElUltimoAdministrador(DatosDeCuenta cuenta, boolean dejaDeSerlo) {
        boolean esAdministradorActivo = cuenta.rol() == Rol.ADMINISTRADOR && cuenta.activa();
        if (esAdministradorActivo && dejaDeSerlo && directorio.activasCon(Rol.ADMINISTRADOR) <= 1) {
            throw new ReglaDeNegocioException(CodigoError.PERSONA_ULTIMO_ADMINISTRADOR,
                    "Es el único administrador activo del club. Nombrá a otro antes de hacer este cambio");
        }
    }

    private void exigirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException(CodigoError.CUENTA_MOTIVO_REQUERIDO,
                    "Para deshabilitar una cuenta hay que indicar el motivo");
        }
    }

    private Long validarRolPersonalizado(Long rolPersonalizadoId, Rol rolBase) {
        if (rolPersonalizadoId == null) {
            return null;
        }
        return roles.personalizadoCompatible(rolPersonalizadoId, rolBase).getId();
    }

    private void asegurarQueAlgoCambia(DatosDeCuenta cuenta, Rol rolNuevo, Long rolPersonalizadoId) {
        boolean mismoRol = cuenta.rol() == rolNuevo;
        boolean mismoPersonalizado = java.util.Objects.equals(cuenta.rolPersonalizadoId(), rolPersonalizadoId);
        if (mismoRol && mismoPersonalizado) {
            throw new ReglaDeNegocioException(CodigoError.ROL_SIN_CAMBIOS,
                    "La persona ya tiene ese rol");
        }
    }

    // ---------- Historial ----------

    private void registrar(DatosDeCuenta cuenta, Long idFinal, MovimientoDeCuenta.Tipo tipo, String detalle,
                           String motivo, Actor actor) {
        MovimientoDeCuenta movimiento = new MovimientoDeCuenta();
        movimiento.setPersonaId(idFinal);
        movimiento.setPersonaIdAnterior(cuenta.id().equals(idFinal) ? null : cuenta.id());
        movimiento.setPersonaNombre(cuenta.nombreCompleto());
        movimiento.setTipo(tipo);
        movimiento.setDetalle(detalle);
        movimiento.setMotivo(motivo);
        movimiento.setHechoPorId(actor.id());
        movimientos.save(movimiento);
    }

    private String descripcionDelCambio(DatosDeCuenta cuenta, Rol rolNuevo, Long rolPersonalizadoId) {
        String texto = cuenta.rol() == rolNuevo
                ? "Siguió siendo " + RolService.etiquetaDe(rolNuevo)
                : "Pasó de " + RolService.etiquetaDe(cuenta.rol()) + " a " + RolService.etiquetaDe(rolNuevo);
        return roles.nombreDe(rolPersonalizadoId)
                .map(nombre -> texto + ", con el rol \"" + nombre + "\"")
                .orElse(texto);
    }

    private String descripcionDeLaBaja(CambioDeEstadoRequest request) {
        return Boolean.TRUE.equals(request.cancelarReservasFuturas())
                ? "Se deshabilitó la cuenta y se cancelaron sus reservas futuras"
                : "Se deshabilitó la cuenta";
    }

    private CuentaResponse aRespuesta(DatosDeCuenta cuenta) {
        return new CuentaResponse(cuenta.id(), cuenta.nombre(), cuenta.apellido(), cuenta.dni(), cuenta.email(),
                cuenta.rol(), cuenta.activa(), cuenta.rolPersonalizadoId(),
                roles.nombreDe(cuenta.rolPersonalizadoId()).orElse(null));
    }
}
