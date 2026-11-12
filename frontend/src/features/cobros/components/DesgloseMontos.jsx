import { etiquetaDescuento, pesos } from "../model/cobro";

/**
 * Monto original, cada descuento aplicado y el total.
 *
 * Es el criterio de aceptación del ticket: si hay un descuento, la pantalla
 * tiene que mostrar las tres cosas, no solo el total. Sin descuentos se
 * muestra una sola línea, para no llenar la pantalla de ceros.
 */
export function DesgloseMontos({ desglose, className = "" }) {
  const { original, aplicados, bonificado, total, hayDescuento } = desglose;

  if (!hayDescuento) {
    return (
      <dl className={`flex items-baseline justify-between gap-3 ${className}`}>
        <dt className="text-sm text-muted-foreground">Total a cobrar</dt>
        <dd className="text-xl font-extrabold tabular-nums">{pesos(total)}</dd>
      </dl>
    );
  }

  return (
    <dl className={`space-y-2 text-sm ${className}`}>
      <div className="flex items-baseline justify-between gap-3">
        <dt className="text-muted-foreground">Monto original</dt>
        <dd className="tabular-nums">{pesos(original)}</dd>
      </div>

      {aplicados.map(({ descuento, bonificacion }) => (
        <div key={descuento.id ?? descuento.nombre} className="flex items-baseline justify-between gap-3">
          <dt className="min-w-0 truncate text-[var(--estado-ok-fg)]">{etiquetaDescuento(descuento)}</dt>
          <dd className="tabular-nums text-[var(--estado-ok-fg)]">- {pesos(bonificacion)}</dd>
        </div>
      ))}

      {aplicados.length > 1 && (
        <div className="flex items-baseline justify-between gap-3">
          <dt className="text-muted-foreground">Descuento total</dt>
          <dd className="tabular-nums">- {pesos(bonificado)}</dd>
        </div>
      )}

      <div className="flex items-baseline justify-between gap-3 border-t border-border pt-2">
        <dt className="font-semibold">Total a cobrar</dt>
        <dd className="text-xl font-extrabold tabular-nums">{pesos(total)}</dd>
      </div>
    </dl>
  );
}
