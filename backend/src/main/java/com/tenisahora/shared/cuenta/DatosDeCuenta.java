package com.tenisahora.shared.cuenta;

import com.tenisahora.shared.model.Rol;

/**
 * Vista de una cuenta que no depende de la tabla en la que está guardada. La usan los casos de uso que trabajan
 * sobre cualquier rol (CU74 asignar rol, CU76 habilitar o deshabilitar) sin conocer las entidades de cada módulo.
 */
public record DatosDeCuenta(Long id, String nombre, String apellido, String dni, String email, Rol rol,
                            boolean activa, Long rolPersonalizadoId) {

    public String nombreCompleto() {
        return nombre + " " + apellido;
    }
}
