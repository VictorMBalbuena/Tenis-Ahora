import { useCallback, useEffect, useRef, useState } from "react";
import { listarEntrenamientos } from "../api/entrenamientosApi";

/** Listado paginado de entrenamientos. Si el usuario es ENTRENADOR, listarEntrenamientos consulta /mis-entrenamientos (CU84). */
export function useEntrenamientos({ estado, dia, entrenadorId, canchaId, fecha, pagina = 0, size = 20 } = {}) {
  const [resultado, setResultado] = useState({
    entrenamientos: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    listarEntrenamientos({ estado, dia, entrenadorId, canchaId, fecha, page: pagina, size })
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            entrenamientos: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({ entrenamientos: [], totalPaginas: 0, totalElementos: 0, cargando: false, error });
        }
      });
  }, [estado, dia, entrenadorId, canchaId, fecha, pagina, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}
