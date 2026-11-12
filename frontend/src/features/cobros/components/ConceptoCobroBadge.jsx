import { Badge } from "@/components/ui/badge";
import { cn } from "cn";
import { ConceptoCobro, ETIQUETA_CONCEPTO } from "../model/cobro";

// El reembolso sale de la caja, así que se distingue del resto por color además del texto.
const CLASES = {
  [ConceptoCobro.SENA]: "bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]",
  [ConceptoCobro.SALDO]: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
  [ConceptoCobro.INSCRIPCION]: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
  [ConceptoCobro.REEMBOLSO]: "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]",
  [ConceptoCobro.OTRO]: "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
};

export function ConceptoCobroBadge({ concepto, className }) {
  if (!concepto) return <span className="text-muted-foreground">—</span>;

  return (
    <Badge className={cn(CLASES[concepto] ?? CLASES[ConceptoCobro.OTRO], className)}>
      {ETIQUETA_CONCEPTO[concepto] ?? concepto}
    </Badge>
  );
}
