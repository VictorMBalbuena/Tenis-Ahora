import { Badge } from "@/components/ui/badge";
import { cn } from "cn";
import { EstadoCobro, ETIQUETA_ESTADO } from "../model/cobro";

// Lo que distingue un cobro pagado de uno rechazado o anulado, por color además del texto.
const CLASES = {
  [EstadoCobro.PENDIENTE]: "bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]",
  [EstadoCobro.PAGADO]: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
  [EstadoCobro.VENCIDO]: "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
  [EstadoCobro.RECHAZADO]: "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]",
  [EstadoCobro.ANULADO]: "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
};

export function EstadoCobroBadge({ estado, className }) {
  if (!estado) return <span className="text-muted-foreground">—</span>;

  return <Badge className={cn(CLASES[estado], className)}>{ETIQUETA_ESTADO[estado] ?? estado}</Badge>;
}
