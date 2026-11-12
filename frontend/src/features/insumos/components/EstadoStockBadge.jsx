import { Badge } from "@/components/ui/badge";
import { cn } from "cn";
import { ETIQUETA_ESTADO, EstadoStock, estadoDeStock } from "../model/insumo";

/**
 * El color no alcanza para comunicar el estado: quien no distingue rojo de
 * verde necesita el texto. Por eso la etiqueta siempre se escribe.
 */
const CLASES = {
  [EstadoStock.DISPONIBLE]:
    "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
  [EstadoStock.STOCK_BAJO]:
    "bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]",
  [EstadoStock.SIN_STOCK]:
    "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]",
};

export function EstadoStockBadge({ insumo, className }) {
  const estado = estadoDeStock(insumo);

  return (
    <Badge className={cn("font-semibold", CLASES[estado], className)}>
      {ETIQUETA_ESTADO[estado]}
    </Badge>
  );
}
