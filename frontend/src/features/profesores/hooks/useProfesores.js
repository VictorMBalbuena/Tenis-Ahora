import { useCallback, useEffect, useRef, useState } from "react";
import { listarProfesores } from "../api/profesoresApi";

/** Listado paginado de profesores, con el mismo control de concurrencia que useInsumos/useCanchas. */
export function useProfesores({ busqueda, estado, activo, pagina = 0, size = 20 }) {
  const [resultado, setResultado] = useState({
    profesores: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    listarProfesores({ busqueda, estado, activo, page: pagina, size })
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            profesores: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({ profesores: [], totalPaginas: 0, totalElementos: 0, cargando: false, error });
        }
      });
  }, [busqueda, estado, activo, pagina, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}

/** Rebota el valor para no pegarle al servidor en cada tecla. */
export function useValorDemorado(valor, ms = 300) {
  const [demorado, setDemorado] = useState(valor);

  useEffect(() => {
    const t = setTimeout(() => setDemorado(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);

  return demorado;
}
