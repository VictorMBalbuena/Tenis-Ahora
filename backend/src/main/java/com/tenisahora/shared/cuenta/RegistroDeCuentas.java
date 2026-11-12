package com.tenisahora.shared.cuenta;

import com.tenisahora.shared.model.Rol;

import java.util.Optional;

/**
 * Puerto que implementa cada módulo de rol sobre sus propias tablas. Auth y los servicios de alta lo usan sin
 * conocer ninguna entidad ni ningún otro módulo.
 */
public interface RegistroDeCuentas {

    /** Si este módulo guarda las cuentas de ese rol. */
    boolean atiende(Rol rol);

    Optional<CuentaDeAcceso> porEmail(String email);

    /** Una sola consulta por clave primaria: quien llama ya sabe el rol por el token. */
    Optional<CuentaDeAcceso> porId(Long id);

    boolean existeDni(String dni);

    boolean existeEmail(String email);

    // ---------- Administración de la cuenta (CU74, CU75, CU76) ----------

    /** Los datos visibles de la cuenta, sin importar en qué tabla esté. */
    Optional<DatosDeCuenta> datosDe(Long id);

    /** Habilita o deshabilita la cuenta (CU76). No borra nada. */
    void cambiarEstado(Long id, boolean activa);

    /** Deja a la cuenta con el rol personalizado indicado, o sin ninguno si llega null (CU74, CU75). */
    void asignarRolPersonalizado(Long id, Long rolPersonalizadoId);

    /** Los datos comunes que viajan a la tabla del rol nuevo (CU74). */
    Optional<DatosTransferibles> datosParaTraspaso(Long id);

    /**
     * Borra la fila de este rol para que la persona pase a otro. Lanza la excepción de historial si quedó
     * referenciada por reservas, inscripciones o asistencias: en ese caso el cambio de rol no es posible.
     */
    void soltarParaTraspaso(Long id);

    /** Crea la cuenta en la tabla de este rol con los datos que venían del rol anterior (CU74). */
    Long recibirEnTraspaso(DatosTransferibles datos);

    /** Cuántas cuentas activas tiene este rol: sirve para no dejar al club sin administradores. */
    long activas();

    /** Cuántas cuentas de este rol usan ese rol personalizado: un rol en uso no se borra. */
    long conRolPersonalizado(Long rolPersonalizadoId);
}
