import { Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "cn";

function generarOpciones(pasoMinutos) {
  const opciones = [];
  for (let m = 0; m < 24 * 60; m += pasoMinutos) {
    const hora = String(Math.floor(m / 60)).padStart(2, "0");
    const minuto = String(m % 60).padStart(2, "0");
    opciones.push(`${hora}:${minuto}`);
  }
  return opciones;
}

/**
 * Selector de hora con nuestro propio estilo (bordes redondeados, acento verde).
 * El desplegable nativo de `<input type="time">` lo dibuja el navegador y no se puede
 * restylear, por eso este componente arma el suyo con DropdownMenu en vez de usarlo.
 *
 * API de input controlado estándar: onChange recibe un evento sintético con
 * target.name/target.value para poder enchufarse directo a un handler tipo
 * `const { name, value } = e.target` sin cambiar nada del lado de quien lo usa.
 */
export function TimeField({ name, value, onChange, pasoMinutos = 30, className }) {
  const opciones = generarOpciones(pasoMinutos);

  const elegir = (hora) => {
    onChange({ target: { name, value: hora, type: "time" } });
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            type="button"
            variant="outline"
            className={cn("w-full justify-between rounded-[20px] px-5 py-3.5 h-auto font-normal text-[15px]", className)}
          >
            <span>{value || "--:--"}</span>
            <Clock size={16} className="text-muted-foreground shrink-0" />
          </Button>
        }
      />
      <DropdownMenuContent className="max-h-60 overflow-y-auto">
        {opciones.map((hora) => (
          <DropdownMenuItem
            key={hora}
            onClick={() => elegir(hora)}
            className={hora === value ? "bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)] font-semibold" : ""}
          >
            {hora}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
