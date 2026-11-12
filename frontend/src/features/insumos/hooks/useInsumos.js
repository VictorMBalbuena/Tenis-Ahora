import { useCallback, useEffect, useRef, useState } from "react";
import { listarCategoriasInsumo, listarInsumos, obtenerResumenInsumos } from "../api/insumosApi";

export function useInsumos({ busqueda, categoriaId, estado, page = 0, size = 20 }) {
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

    listarInsumos({ busqueda, categoriaId, estado, page, size })
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
  }, [busqueda, categoriaId, estado, page, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}

export function useResumenInsumos() {
  const [resumen, setResumen] = useState({ distintos: 0, enDeposito: 0, asignadas: 0, alertas: 0 });

  const recargar = useCallback(() => {
    return obtenerResumenInsumos().then(setResumen);
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { resumen, recargarResumen: recargar };
}

export function useCategoriasInsumo() {
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);

  const recargar = useCallback(() => {
    return listarCategoriasInsumo().then((data) => {
      setCategorias(data);
      setCargando(false);
    });
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { categorias, cargando, recargarCategorias: recargar };
}

export function useValorDemorado(valor, ms = 300) {
  const [demorado, setDemorado] = useState(valor);

  useEffect(() => {
    const timer = setTimeout(() => setDemorado(valor), ms);
    return () => clearTimeout(timer);
  }, [valor, ms]);

  return demorado;
}
