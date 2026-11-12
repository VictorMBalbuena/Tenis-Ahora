import { useCallback, useEffect, useRef, useState } from "react";
import axiosClient from "@/config/axiosClient";

/**
 * Hook para la grilla de disponibilidad de un día: trae las canchas y, para
 * cada una, sus franjas horarias de esa fecha en paralelo.
 */
export function useDisponibilidad(fecha) {
  const [resultado, setResultado] = useState({
    canchas: [],
    grillaData: {},
    cargando: true,
    error: false,
  });
  const pedido = useRef(0);

  const recargar = useCallback(() => {
    const miPedido = ++pedido.current;

    axiosClient
      .get("/canchas", { params: { page: 0, size: 50 } })
      .then(async ({ data: dataCanchas }) => {
        const listaCanchas = dataCanchas.contenido || [];

        const resultados = await Promise.all(
          listaCanchas.map((cancha) =>
            axiosClient
              .get(`/canchas/${cancha.id}/disponibilidad`, { params: { desde: fecha, hasta: fecha } })
              .then(({ data }) => ({ canchaId: cancha.id, data }))
              .catch(() => ({ canchaId: cancha.id, data: null }))
          )
        );

        if (miPedido !== pedido.current) return;

        const grillaData = resultados.reduce((acc, { canchaId, data }) => {
          acc[canchaId] = data;
          return acc;
        }, {});

        setResultado({ canchas: listaCanchas, grillaData, cargando: false, error: false });
      })
      .catch((error) => {
        console.error("Error al cargar la grilla:", error);
        if (miPedido === pedido.current) {
          setResultado((prev) => ({ ...prev, cargando: false, error: true }));
        }
      });
  }, [fecha]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}
