import { Badge } from "@/components/ui/badge";
import { cn } from "cn";

/**
 * Las categorías son dinámicas (el club las crea), no un enum fijo con un
 * color por valor como la superficie de Canchas. Por eso van todas con el
 * mismo estilo neutro, en vez de un mapa de clases por categoría.
 */
export function CategoriaInsumoBadge({ categoria, className }) {
  if (!categoria) return null;

  return (
    <Badge className={cn("bg-[var(--estado-neutro-bg)] font-semibold text-[var(--estado-neutro-fg)]", className)}>
      {categoria.nombre}
    </Badge>
  );
}
