/**
 * Persona (@MappedSuperclass, sin tabla propia): id, nombre y apellido que comparten los cinco roles
 * (socio, empleado, administrador, profesor, entrenador). El id sale de una secuencia única y compartida
 * (persona_id_seq), así que es único entre todas las tablas de rol. EmpleadoBase agrega lo común a los roles
 * de personal. No hay auditoría de JPA habilitada: las fechas de alta o modificación las pone cada entidad
 * que las necesita, a mano.
 */
package com.tenisahora.shared.entity;
