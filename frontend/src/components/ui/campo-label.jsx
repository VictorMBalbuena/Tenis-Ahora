import { Info } from "lucide-react";
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip";

/** Ícono de info con tooltip al tocarlo: la ayuda no va como texto fijo pegado a la card. */
export function InfoTip({ children }) {
  return (
    <Tooltip>
      <TooltipTrigger
        type="button"
        className="text-muted-foreground hover:text-primary transition-colors"
        aria-label="Más información"
      >
        <Info size={14} />
      </TooltipTrigger>
      <TooltipContent>{children}</TooltipContent>
    </Tooltip>
  );
}

export function CampoLabel({ children, info, htmlFor }) {
  return (
    <label htmlFor={htmlFor} className="flex items-center gap-1.5 text-[14px] font-semibold text-foreground">
      {children}
      {info && <InfoTip>{info}</InfoTip>}
    </label>
  );
}
