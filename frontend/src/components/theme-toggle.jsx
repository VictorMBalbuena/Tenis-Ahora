import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tema, esOscuro } from "@/lib/tema";
import { useTema } from "@/hooks/use-tema";

/** Atajo de la barra superior: alterna claro/oscuro sin pasar por Configuración. */
export function ThemeToggle() {
  const { tema, cambiarTema } = useTema();
  const oscuro = esOscuro(tema);

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={() => cambiarTema(oscuro ? Tema.CLARO : Tema.OSCURO)}
      aria-label={oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro"}
    >
      {oscuro ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </Button>
  );
}
