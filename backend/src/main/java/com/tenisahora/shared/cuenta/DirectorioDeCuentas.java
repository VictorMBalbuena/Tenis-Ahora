package com.tenisahora.shared.cuenta;

import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Recorre los módulos de rol. El DNI y el correo son únicos en todo el club aunque cada rol tenga su tabla. */
@Component
@RequiredArgsConstructor
public class DirectorioDeCuentas {

    private final List<RegistroDeCuentas> registros;

    /** Solo en el inicio de sesión: todavía no se sabe de qué rol es. */
    public Optional<CuentaDeAcceso> porEmail(String email) {
        return registros.stream().map(registro -> registro.porEmail(email)).flatMap(Optional::stream).findFirst();
    }

    /** Pedidos autenticados: el token trae el rol, así que se consulta solo la tabla que corresponde. */
    public Optional<CuentaDeAcceso> porId(Long id, Rol rol) {
        return registros.stream().filter(registro -> registro.atiende(rol))
                .map(registro -> registro.porId(id)).flatMap(Optional::stream)
                .filter(cuenta -> cuenta.rol() == rol).findFirst();
    }

    public void asegurarDniLibre(String dni) {
        if (registros.stream().anyMatch(registro -> registro.existeDni(dni))) {
            throw new ConflictoException(CodigoError.PERSONA_DNI_DUPLICADO, "Ya existe una persona con ese DNI");
        }
    }

    public void asegurarEmailLibre(String email) {
        if (registros.stream().anyMatch(registro -> registro.existeEmail(email))) {
            throw new ConflictoException(CodigoError.PERSONA_EMAIL_DUPLICADO,
                    "Ya existe una persona con ese correo electrónico");
        }
    }

    // ---------- Administración de cuentas (CU74, CU75, CU76) ----------

    /**
     * Busca la cuenta en los cinco roles. Es la única consulta del sistema que no sabe de antemano en qué tabla
     * está la persona, y la necesitan los casos de uso que trabajan sobre cualquier rol.
     */
    public Optional<DatosDeCuenta> buscar(Long id) {
        return registros.stream().map(registro -> registro.datosDe(id)).flatMap(Optional::stream).findFirst();
    }

    public DatosDeCuenta exigir(Long id) {
        return buscar(id).orElseThrow(() ->
                new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));
    }

    /** CU76: habilitar o deshabilitar sin borrar nada. */
    public void cambiarEstado(Long id, Rol rol, boolean activa) {
        del(rol).cambiarEstado(id, activa);
    }

    /** CU74 y CU75: deja la cuenta apuntando a un rol personalizado, o a ninguno si llega null. */
    public void asignarRolPersonalizado(Long id, Rol rol, Long rolPersonalizadoId) {
        del(rol).asignarRolPersonalizado(id, rolPersonalizadoId);
    }

    /**
     * CU74: mueve la cuenta de la tabla de su rol actual a la del rol nuevo, conservando los datos de Persona y la
     * contraseña. Devuelve el id nuevo.
     *
     * <p>El id cambia porque cada rol es una tabla distinta. No rompe nada: si la persona tuviera historial, el
     * paso anterior (soltar la fila vieja) habría fallado con PERSONA_CON_HISTORIAL y el cambio no se haría.
     */
    public Long traspasar(Long id, Rol desde, Rol hacia) {
        DatosTransferibles datos = del(desde).datosParaTraspaso(id).orElseThrow(() ->
                new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));

        del(desde).soltarParaTraspaso(id);
        return del(hacia).recibirEnTraspaso(datos);
    }

    /** Cuántas cuentas activas tiene un rol. Se usa para no dejar al club sin administradores. */
    public long activasCon(Rol rol) {
        return del(rol).activas();
    }

    /** Cuántas personas, de cualquier rol, están usando ese rol personalizado. */
    public long cuantasUsan(Long rolPersonalizadoId) {
        return registros.stream().mapToLong(registro -> registro.conRolPersonalizado(rolPersonalizadoId)).sum();
    }

    private RegistroDeCuentas del(Rol rol) {
        return registros.stream().filter(registro -> registro.atiende(rol)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay módulo para el rol " + rol));
    }
}
