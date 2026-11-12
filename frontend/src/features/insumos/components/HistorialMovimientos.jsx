import { ETIQUETA_MOVIMIENTO, fechaCorta } from "../model/insumo";

/** Últimos movimientos de stock de un insumo, en la página de Detalle. */
export function HistorialMovimientos({ movimientos }) {
  if (movimientos.length === 0) {
    return (
      <p className="rounded-lg border border-border p-3 text-sm text-muted-foreground">
        Todavía no hay movimientos registrados.
      </p>
    );
  }

  return (
    <ul className="space-y-2.5">
      {movimientos.map((m) => {
        const bajo = m.variacion < 0;
        return (
          <li key={m.id} className="flex items-center justify-between gap-3 rounded-lg border border-border p-3">
            <div className="min-w-0">
              <p className="text-sm font-medium">{ETIQUETA_MOVIMIENTO[m.tipo] ?? m.tipo}</p>
              <p className="truncate text-xs text-muted-foreground">{m.motivo || "Sin motivo registrado"}</p>
            </div>
            <div className="flex shrink-0 items-center gap-3">
              <span className="font-mono text-xs tabular-nums text-muted-foreground">{fechaCorta(m.fecha)}</span>
              <span
                className={`rounded-full px-2 py-0.5 text-xs font-semibold tabular-nums ${
                  bajo
                    ? "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]"
                    : "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]"
                }`}
              >
                {m.variacion > 0 ? `+${m.variacion}` : m.variacion}
              </span>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
