import { useCallback, useEffect, useRef, useState } from "react";
import { listarEntrenadores } from "../api/entrenadoresApi";

/** Listado paginado de entrenadores, con el mismo control de concurrencia que useProfesores/useInsumos/useCanchas. */
export function useEntrenadores({ busqueda, especialidad, estado, activo, pagina = 0, size = 20 }) {
  const [resultado, setResultado] = useState({
    entrenadores: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;
    const busquedaLimpia = busqueda ? busqueda.trim() : "";

    // Evita peticiones inválidas al backend mientras el usuario escribe (mínimo 2 caracteres)
    if (busquedaLimpia && busquedaLimpia.length < 2) {
      if (miPedido === pedido.current) {
        setResultado({
          entrenadores: [],
          totalPaginas: 0,
          totalElementos: 0,
          cargando: false,
          error: null,
        });
      }
      return;
    }

    setResultado((prev) => ({ ...prev, cargando: true, error: null }));

    listarEntrenadores({ busqueda: busquedaLimpia, especialidad, estado, activo, page: pagina, size })
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            entrenadores: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({ entrenadores: [], totalPaginas: 0, totalElementos: 0, cargando: false, error });
        }
      });
  }, [busqueda, especialidad, estado, activo, pagina, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}
