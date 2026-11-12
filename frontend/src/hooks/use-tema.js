import { useCallback, useEffect, useState } from "react";
import { Tema, aplicarTema, escucharSistema, guardarTema, temaGuardado } from "@/lib/tema";

/**
 * Tema elegido y cómo cambiarlo.
 *
 * El tema se aplica al elegirlo, no en un efecto: así no queda la pantalla
 * un instante con el tema anterior.
 */
export function useTema() {
  const [tema, setTema] = useState(temaGuardado);

  const cambiarTema = useCallback((nuevo) => {
    setTema(nuevo);
    guardarTema(nuevo);
    aplicarTema(nuevo);
  }, []);

  // Con "el del sistema" hay que seguir al sistema mientras la página está abierta.
  useEffect(() => {
    if (tema !== Tema.SISTEMA) return undefined;
    return escucharSistema(() => aplicarTema(Tema.SISTEMA));
  }, [tema]);

  return { tema, cambiarTema };
}
