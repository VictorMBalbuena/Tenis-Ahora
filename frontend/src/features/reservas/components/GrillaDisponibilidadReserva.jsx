import { formatearMoneda, turnoDisponible } from "../model/reserva";

/**
 * Grilla interactiva de disponibilidad de canchas y turnos para el wizard de reservas.
 * Sigue el diseño de `reserva-01-turno.html` (L262-302).
 */
export function GrillaDisponibilidadReserva({
  canchas = [],
  seleccion = null,
  onSeleccionarTurno = () => {},
  cargando = false,
  error = null,
  duracionHoras = 1,
  fecha = null,
  soloConsulta = false,
}) {
  if (cargando) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground animate-pulse">
        Cargando disponibilidad de canchas...
      </div>
    );
  }

  if (error) {
    return (
      <div
        role="alert"
        className="p-4 rounded-xl border border-destructive/20 bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)] text-sm font-medium"
      >
        {error}
      </div>
    );
  }

  if (!canchas || canchas.length === 0) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground">
        No hay canchas disponibles para la fecha seleccionada.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Referencias de estado visual según mockup */}
      <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pb-1">
        <span className="inline-flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-xs border border-input bg-background" />
          Libre
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-xs bg-primary" />
          Seleccionado
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-xs bg-muted border border-input line-through" />
          Ocupada
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-xs bg-muted/50 border border-input" />
          Mantenimiento
        </span>
      </div>

      <div className="space-y-4">
        {canchas.map((cancha) => {
          const idCancha = cancha.canchaId ?? cancha.id;
          const nombreCancha = cancha.identificador || cancha.nombre || `Cancha ${idCancha}`;
          const precio = cancha.precioPorHora;

          return (
            <div
              key={idCancha}
              className="rounded-xl border border-border bg-card p-4 shadow-2xs space-y-3"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="font-semibold text-foreground text-sm sm:text-base">
                    {nombreCancha}
                  </h4>
                  {cancha.tipoSuperficie && (
                    <span className="text-xs text-muted-foreground capitalize">
                      {cancha.tipoSuperficie.toLowerCase()}
                    </span>
                  )}
                </div>
                {precio != null && (
                  <span className="text-xs sm:text-sm font-semibold tabular-nums text-foreground">
                    {formatearMoneda(precio)} / hora
                  </span>
                )}
              </div>

              <div className="grid grid-cols-3 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-8 gap-2">
                {(cancha.franjas || []).map((franja, idx) => {
                  const hora = franja.desde ? franja.desde.slice(0, 5) : "";
                  const esLibre = turnoDisponible(cancha.franjas, idx, { duracionHoras, fecha });
                  const libreSinAlcanzar = !esLibre && franja.estado === "LIBRE";
                  const esOcupada = franja.estado === "OCUPADA";
                  const estaSeleccionado =
                    seleccion?.canchaId === idCancha && seleccion?.horaInicio === hora;

                  if (esLibre) {
                    return (
                      <button
                        key={`${idCancha}-${hora}-${idx}`}
                        type="button"
                        role="button"
                        aria-label={`Turno ${hora} en ${nombreCancha}`}
                        aria-pressed={estaSeleccionado}
                        disabled={soloConsulta}
                        onClick={() =>
                          onSeleccionarTurno({
                            canchaId: idCancha,
                            canchaNombre: nombreCancha,
                            precioPorHora: precio,
                            horaInicio: hora,
                          })
                        }
                        className={`h-10 rounded-md border text-sm font-medium tabular-nums transition-colors disabled:pointer-events-none disabled:opacity-60 cursor-pointer ${
                          estaSeleccionado
                            ? "border-primary bg-primary text-primary-foreground font-bold shadow-xs"
                            : "border-input bg-background hover:border-primary hover:bg-[var(--estado-brand-bg)]"
                        }`}
                      >
                        {hora}
                      </button>
                    );
                  }

                  return (
                    <button
                      key={`${idCancha}-${hora}-${idx}`}
                      type="button"
                      role="button"
                      disabled
                      aria-disabled="true"
                      aria-label={`Turno ${hora} ${libreSinAlcanzar ? "no disponible" : esOcupada ? "ocupado" : "en mantenimiento"}`}
                      className={`h-10 rounded-md border text-sm font-medium tabular-nums transition-colors cursor-not-allowed ${
                        esOcupada
                          ? "border-input bg-muted text-muted-foreground/60 line-through"
                          : "border-input bg-muted/50 text-muted-foreground/40"
                      }`}
                    >
                      {hora}
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default GrillaDisponibilidadReserva;
