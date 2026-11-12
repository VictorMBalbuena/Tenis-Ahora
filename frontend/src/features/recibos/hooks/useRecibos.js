import { useCallback, useEffect, useRef, useState } from "react";
import { listarRecibos } from "../api/recibosApi";

/** Listado paginado de recibos (CU59). */
export function useRecibos({ busqueda, desde, hasta, pagina = 0, size = 20 }) {
  const [resultado, setResultado] = useState({
    recibos: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    listarRecibos({ busqueda, desde, hasta, page: pagina, size })
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            recibos: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({ recibos: [], totalPaginas: 0, totalElementos: 0, cargando: false, error });
        }
      });
  }, [busqueda, desde, hasta, pagina, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}
