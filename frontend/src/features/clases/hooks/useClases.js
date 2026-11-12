import { useCallback, useEffect, useRef, useState } from "react";
import { listarClases } from "../api/clasesApi";

/** Listado paginado de clases... */
export function useClases({ nivel, estado, profesorId, canchaId, fecha, dia, pagina = 0, size = 20, esProfesor = false }) { // <-- AGREGADO AL HOOK
  const [resultado, setResultado] = useState({
    clases: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    listarClases({ nivel, estado, profesorId, canchaId, fecha, dia, page: pagina, size, esProfesor }) // <-- ENVIADO A LA API
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            clases: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({ clases: [], totalPaginas: 0, totalElementos: 0, cargando: false, error });
        }
      });
  }, [nivel, estado, profesorId, canchaId, fecha, dia, pagina, size, esProfesor]); // <-- AGREGADO AL HOOK

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}
