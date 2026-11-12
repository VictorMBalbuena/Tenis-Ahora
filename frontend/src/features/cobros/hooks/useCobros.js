import { useCallback, useEffect, useRef, useState } from "react";
import { listarCobros } from "../api/cobrosApi";

/** Listado paginado de cobros (CU54), con el mismo control de concurrencia que useEntrenadores/useInsumos. */
export function useCobros({ busqueda, concepto, medioPago, desde, hasta, pagina = 0, size = 20, omitir = false }) {
  const [resultado, setResultado] = useState({
    cobros: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    listarCobros({ busqueda, concepto, medioPago, desde, hasta, page: pagina, size })
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            cobros: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({ cobros: [], totalPaginas: 0, totalElementos: 0, cargando: false, error });
        }
      });
  }, [busqueda, concepto, medioPago, desde, hasta, pagina, size]);

  useEffect(() => {
    if (!omitir) recargar();
  }, [recargar, omitir]);

  return { ...resultado, recargar };
}
