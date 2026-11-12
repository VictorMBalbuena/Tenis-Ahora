import { Monitor, Moon, Sun } from "lucide-react";
import { ETIQUETA_TEMA, Tema } from "@/lib/tema";
import { useTema } from "@/hooks/use-tema";

const OPCIONES = [
  { valor: Tema.CLARO, icono: Sun, detalle: "Siempre claro." },
  { valor: Tema.OSCURO, icono: Moon, detalle: "Siempre oscuro." },
  { valor: Tema.SISTEMA, icono: Monitor, detalle: "Sigue al sistema operativo." },
];

/**
 * Elección del tema.
 *
 * Es lo único de esta pantalla que no pasa por el backend: se guarda en el
 * navegador, porque es una preferencia del dispositivo y no de la cuenta.
 */
export function TemaCard() {
  const { tema, cambiarTema } = useTema();

  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <div>
        <h2 className="text-lg font-bold text-foreground">Apariencia</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Se guarda en este dispositivo, así que podés tener el mostrador en claro y tu teléfono en oscuro.
        </p>
      </div>

      <fieldset>
        <legend className="sr-only">Tema de la interfaz</legend>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          {OPCIONES.map(({ valor, icono: Icono, detalle }) => {
            const elegido = tema === valor;
            return (
              <label
                key={valor}
                className={`flex cursor-pointer flex-col gap-2 rounded-xl border p-4 transition-colors ${
                  elegido ? "border-primary bg-primary/10" : "border-border hover:bg-muted"
                }`}
              >
                <span className="flex items-center gap-2.5">
                  <input
                    type="radio"
                    name="tema"
                    value={valor}
                    checked={elegido}
                    onChange={() => cambiarTema(valor)}
                    className="size-4 shrink-0 accent-primary"
                  />
                  <Icono className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-sm font-semibold">{ETIQUETA_TEMA[valor]}</span>
                </span>
                <span className="pl-[1.65rem] text-xs text-muted-foreground">{detalle}</span>
              </label>
            );
          })}
        </div>
      </fieldset>
    </div>
  );
}
