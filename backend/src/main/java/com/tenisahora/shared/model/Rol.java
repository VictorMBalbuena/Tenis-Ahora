package com.tenisahora.shared.model;

/**
 * Lista fija de roles: no hay tabla Rol ni permisos configurables. Lo que puede hacer cada rol sale de los casos de
 * uso de cada actor, no de datos en la base. Cada rol tiene su propia tabla (socio, empleado, administrador,
 * profesor, entrenador), así que el rol de una cuenta lo da la tabla donde está.
 */
public enum Rol {
    SOCIO,
    EMPLEADO,
    ADMINISTRADOR,
    PROFESOR,
    ENTRENADOR
}
