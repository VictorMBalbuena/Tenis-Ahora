import { useCallback, useEffect, useRef, useState } from "react";
import { listarDescuentos } from "../api/descuentosApi";

/**
 * Hook para listar descuentos con paginado, filtros y control estricto de
 * concurrencia (descarte de respuestas obsoletas).
 *
 * Mismo patron que useUsuarios y useInsumos: el estado se actualiza solo
 * dentro del `then`, nunca de forma sincronica en el cuerpo del efecto.
 */
export function useDescuentos({ busqueda, activo, page = 0, size = 20 }) {
  const [resultado, setResultado] = useState({
    contenido: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: null,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    listarDescuentos({ busqueda, activo, page, size })
      .then((data) => {
        if (miPedido === pedido.current) {
          setResultado({
            contenido: data.contenido || [],
            totalPaginas: data.totalPaginas ?? 0,
            totalElementos: data.totalElementos ?? 0,
            cargando: false,
            error: null,
          });
        }
      })
      .catch((error) => {
        if (miPedido === pedido.current) {
          setResultado({
            contenido: [],
            totalPaginas: 0,
            totalElementos: 0,
            cargando: false,
            error,
          });
        }
      });
  }, [busqueda, activo, page, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}

/** Rebota el valor para no pegarle al servidor en cada tecla. */
export function useValorDemorado(valor, ms = 300) {
  const [demorado, setDemorado] = useState(valor);

  useEffect(() => {
    const timer = setTimeout(() => setDemorado(valor), ms);
    return () => clearTimeout(timer);
  }, [valor, ms]);

  return demorado;
}
