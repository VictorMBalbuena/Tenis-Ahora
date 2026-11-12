package com.tenisahora.shared.event;

/**
 * Se publica justo antes de borrar a un usuario. Otros módulos (por ejemplo auth, con sus sesiones) lo escuchan
 * para limpiar lo suyo sin que el módulo usuario dependa de ellos.
 */
public record PersonaEliminadaEvent(Long personaId) {
}
