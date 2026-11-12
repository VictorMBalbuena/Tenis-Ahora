import { porcentajeDisponible, stockTotal } from "../model/insumo";

/**
 * Disponible sobre total, con la barra como refuerzo visual.
 *
 * La barra es decorativa: el dato ya esta escrito arriba en numeros, asi que
 * se oculta de los lectores de pantalla en vez de duplicar la lectura.
 */
export function BarraStock({ insumo }) {
  const total = stockTotal(insumo);
  const porcentaje = porcentajeDisponible(insumo);

  return (
    <div className="min-w-[110px] space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="font-medium tabular-nums">{insumo.cantidadDisponible}</span>
        <span className="text-muted-foreground tabular-nums">/ {total}</span>
      </div>
      <div
        aria-hidden="true"
        className="h-1.5 w-full overflow-hidden rounded-full bg-muted"
      >
        <div
          className="h-full rounded-full bg-primary transition-[width]"
          style={{ width: `${porcentaje}%` }}
        />
      </div>
    </div>
  );
}
