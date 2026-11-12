import { useCallback, useEffect, useRef, useState } from "react";
import axiosClient from "@/config/axiosClient";

/**
 * Hook para listar canchas con paginado y filtros, con el mismo control de
 * concurrencia que useInsumos: solo se pisa el estado con la respuesta del
 * último pedido en vuelo.
 */
export function useCanchas({ busqueda, superficie, estado, pagina = 0, size = 20 }) {
  const [resultado, setResultado] = useState({
    canchas: [],
    totalPaginas: 0,
    totalElementos: 0,
    cargando: true,
    error: false,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;
    const params = { page: pagina, size };
    if (busqueda) params.identificador = busqueda;
    if (superficie) params.superficie = superficie;
    if (estado) params.estado = estado;

    axiosClient
      .get("/canchas", { params })
      .then(({ data }) => {
        if (miPedido === pedido.current) {
          setResultado({
            canchas: data.contenido || [],
            totalPaginas: data.totalPaginas || 0,
            totalElementos: data.totalElementos || 0,
            cargando: false,
            error: false,
          });
        }
      })
      .catch((error) => {
        console.error("Error al cargar las canchas:", error);
        if (miPedido === pedido.current) {
          setResultado((prev) => ({ ...prev, cargando: false, error: true }));
        }
      });
  }, [busqueda, superficie, estado, pagina, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  /** Baja optimista: saca la cancha ya eliminada sin volver a pedir la página. */
  const quitarCancha = useCallback((id) => {
    setResultado((prev) => ({ ...prev, canchas: prev.canchas.filter((c) => c.id !== id) }));
  }, []);

  return { ...resultado, quitarCancha, recargar };
}
