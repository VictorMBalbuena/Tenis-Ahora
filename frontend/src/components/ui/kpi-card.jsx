// Indicador de las pantallas: la etiqueta a la izquierda y el número a la derecha.
export function KpiCard({ etiqueta, valor, detalle, icono: Icono, valorClase = "", detalleClase = "text-muted-foreground" }) {
  return (
    <div className="min-w-0 rounded-xl border border-border bg-card p-4 shadow-xs">
      <div className="flex min-w-0 items-center gap-2 text-xs text-muted-foreground">
        {Icono && <Icono className="size-3.5 shrink-0" />}
        <span className="min-w-0">{etiqueta}</span>
      </div>
      <p className={`mt-1 text-right font-mono text-2xl font-semibold tabular-nums ${valorClase}`}>{valor}</p>
      {detalle && <p className={`mt-0.5 text-right text-xs ${detalleClase}`}>{detalle}</p>}
    </div>
  );
}
