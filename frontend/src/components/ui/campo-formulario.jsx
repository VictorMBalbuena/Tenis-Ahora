import { Info } from "lucide-react";
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip";

// Campo de formulario con su error debajo; mismo estilo que NuevaCanchaPage/NuevoInsumoPage (input grande tipo píldora).
export const CLASE_INPUT = "w-full px-5 py-3.5 border border-input rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]";

export function CampoFormulario({ id, etiqueta, info, ayuda, error, ancho = "", obligatorio = false, children }) {
  return (
    <div className={`space-y-2 ${ancho}`}>
      <div className="flex items-center gap-1.5">
        <label htmlFor={id} className="text-[14px] font-semibold text-foreground">
          {etiqueta}
        </label>
        {obligatorio && <span className="text-destructive font-bold" aria-hidden="true">*</span>}
        {info && (
          <Tooltip>
            <TooltipTrigger type="button" className="text-muted-foreground hover:text-primary transition-colors" aria-label="Más información">
              <Info size={14} />
            </TooltipTrigger>
            <TooltipContent>{info}</TooltipContent>
          </Tooltip>
        )}
      </div>
      {children({ id })}
      {ayuda && !error && <p className="text-xs text-muted-foreground">{ayuda}</p>}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
