import { Minus, Plus, Package } from "lucide-react";
import { formatearMoneda } from "../model/reserva";

/**
 * Componente presentacional para listar y modificar la cantidad de insumos deportivos disponibles.
 * Sigue fielmente reserva-03-insumos.html y reserva-03-sin-stock.html.
 */
export function SelectorInsumos({
  insumos = [],
  cantidades = {},
  onCambiarCantidad = () => {},
  cargando = false,
  error = null,
}) {
  if (cargando) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground animate-pulse">
        Cargando insumos disponibles para el turno...
      </div>
    );
  }

  if (error) {
    return (
      <div
        role="alert"
        className="p-4 rounded-xl border border-[var(--estado-error-border)] bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)] text-sm font-medium"
      >
        {error}
      </div>
    );
  }

  if (!insumos || insumos.length === 0) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground">
        No hay insumos disponibles para este turno. Podés continuar con la reserva de la cancha.
      </div>
    );
  }

  return (
    <div className="space-y-2.5">
      {insumos.map((insumo) => {
        const insumoId = insumo.insumoId ?? insumo.id;
        const nombre = insumo.nombre || "Insumo";
        const cantidadAsignable = Number(insumo.cantidadAsignable ?? 0);
        const sinStock = cantidadAsignable <= 0;
        const cantidadActual = Number(cantidades[insumoId] || 0);

        return (
          <div
            key={insumoId}
            className={`flex items-center gap-3 rounded-lg border p-3.5 transition-colors ${
              sinStock && cantidadActual === 0
                ? "border-border/60 bg-muted/20"
                : "border-border bg-card"
            }`}
          >
            {/* Ícono en contenedor según mockup L241 */}
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
              <Package className="h-[18px] w-[18px] shrink-0" />
            </span>

            {/* Información del insumo */}
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 flex-wrap">
                <p className="text-sm font-semibold text-foreground">
                  {nombre}
                </p>
                {insumo.categoria?.nombre && (
                  <span className="inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2 py-0.5 text-xs bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
                    {insumo.categoria.nombre}
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                {sinStock ? (
                  <span
                    role="status"
                    className="font-medium text-[var(--estado-error-fg)]"
                  >
                    Sin stock disponible
                  </span>
                ) : (
                  <span>
                    {cantidadAsignable} {cantidadAsignable === 1 ? "disponible" : "disponibles"} en stock
                  </span>
                )}

                {insumo.precioUnitario != null && (
                  <>
                    <span>&middot;</span>
                    <span className="font-mono tabular-nums">
                      {formatearMoneda(insumo.precioUnitario)} / unidad
                    </span>
                  </>
                )}
              </div>
            </div>

            {/* Stepper de unidades circular según mockup L247-249 */}
            <div className="flex shrink-0 items-center gap-1.5">
              <button
                type="button"
                role="button"
                aria-label={`disminuir ${nombre}`}
                disabled={cantidadActual <= 0}
                onClick={() => onCambiarCantidad(insumoId, Math.max(0, cantidadActual - 1))}
                className="inline-flex items-center justify-center rounded-full border border-input bg-background hover:bg-muted h-7 w-7 p-0 text-sm font-semibold transition-colors disabled:pointer-events-none disabled:opacity-40 cursor-pointer"
              >
                <Minus className="w-3.5 h-3.5" />
              </button>

              <span
                aria-label={`cantidad de ${nombre}`}
                className="w-7 text-center font-mono font-medium text-sm tabular-nums text-foreground select-none"
              >
                {cantidadActual}
              </span>

              <button
                type="button"
                role="button"
                aria-label={`aumentar ${nombre}`}
                disabled={sinStock || cantidadActual >= cantidadAsignable}
                onClick={() =>
                  onCambiarCantidad(insumoId, Math.min(cantidadAsignable, cantidadActual + 1))
                }
                className="inline-flex items-center justify-center rounded-full border border-input bg-background hover:bg-muted h-7 w-7 p-0 text-sm font-semibold transition-colors disabled:pointer-events-none disabled:opacity-40 cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default SelectorInsumos;
