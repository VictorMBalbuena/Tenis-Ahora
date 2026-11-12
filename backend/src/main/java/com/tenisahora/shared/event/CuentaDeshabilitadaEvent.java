package com.tenisahora.shared.event;

/**
 * Se publica al deshabilitar una cuenta (CU76). Lo escucha auth, para cerrarle las sesiones abiertas, y reservas,
 * para cancelarle los turnos futuros si el administrador lo pidió.
 *
 * <p>Va como evento y no como llamada directa por lo mismo que {@link PersonaEliminadaEvent}: el módulo de
 * permisos no tiene por qué conocer ni a auth ni a reservas.
 */
public record CuentaDeshabilitadaEvent(Long personaId, boolean cancelarReservasFuturas, String motivo) {
}
