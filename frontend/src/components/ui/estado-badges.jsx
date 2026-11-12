import { Badge } from "@/components/ui/badge";
import { cn } from "cn";

// Las dos etiquetas de estado de un instructor (profesor/entrenador): su color no alcanza para comunicar el estado, por eso siempre llevan texto.

export function ValidacionBadge({ persona, className }) {
  const vencida = persona.estadoValidacion === "VALIDADO" && persona.certificacion && !persona.certificacion.vigente;
  if (vencida) {
    return <Badge className={cn("bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]", className)}>Vencida</Badge>;
  }

  const CLASES = {
    VALIDADO: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
    PENDIENTE: "bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]",
    RECHAZADA: "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]",
  };
  const ETIQUETAS = {
    VALIDADO: "Validada",
    PENDIENTE: "Pendiente de validar",
    RECHAZADA: "Rechazada",
  };
  const estado = persona.estadoValidacion;

  return <Badge className={cn(CLASES[estado], className)}>{ETIQUETAS[estado]}</Badge>;
}

export function ActivoBadge({ persona, className }) {
  return (
    <Badge
      className={cn(
        persona.activo
          ? "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]"
          : "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
        className,
      )}
    >
      {persona.activo ? "Activo" : "Inactivo"}
    </Badge>
  );
}
