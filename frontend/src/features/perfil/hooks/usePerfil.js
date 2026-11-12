import { useCallback, useEffect, useRef, useState } from "react";
import { obtenerMiPerfil } from "../api/perfilApi";

/** El perfil propio, con el mismo control de concurrencia que los listados. */
export function usePerfil() {
  const [resultado, setResultado] = useState({ perfil: null, cargando: true, error: null });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    obtenerMiPerfil()
      .then((perfil) => {
        if (miPedido === pedido.current) setResultado({ perfil, cargando: false, error: null });
      })
      .catch((error) => {
        if (miPedido === pedido.current) setResultado({ perfil: null, cargando: false, error });
      });
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, setPerfil: (perfil) => setResultado((prev) => ({ ...prev, perfil })), recargar };
}
