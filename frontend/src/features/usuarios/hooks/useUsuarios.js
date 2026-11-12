import { useCallback, useEffect, useRef, useState } from "react";
import { listarUsuarios } from "../api/usuariosApi";

/**
 * Hook personalizado para listar usuarios con paginado, filtros y control
 * estricto de concurrencia (descarte de respuestas obsoletas).
 */
export function useUsuarios({ tipo, busqueda, page = 0, size = 20 }) {
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

    const filtros = { page, size };
    const busquedaLimpia = busqueda ? busqueda.trim() : "";

    if (busquedaLimpia) {
      if (/^\d+$/.test(busquedaLimpia)) {
        if (busquedaLimpia.length >= 7 && busquedaLimpia.length <= 8) {
          filtros.dni = busquedaLimpia;
        } else {
          // Si el DNI está incompleto (< 7 dígitos) no enviamos petición inválida al backend
          if (miPedido === pedido.current) {
            setResultado({
              contenido: [],
              totalPaginas: 0,
              totalElementos: 0,
              cargando: false,
              error: null,
            });
          }
          return;
        }
      } else {
        if (busquedaLimpia.length >= 2) {
          filtros.nombre = busquedaLimpia;
        } else {
          // Búsqueda por nombre requiere al menos 2 letras en el backend
          if (miPedido === pedido.current) {
            setResultado({
              contenido: [],
              totalPaginas: 0,
              totalElementos: 0,
              cargando: false,
              error: null,
            });
          }
          return;
        }
      }
    }

    setResultado((prev) => ({ ...prev, cargando: true, error: null }));

    listarUsuarios(tipo, filtros)
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
  }, [tipo, busqueda, page, size]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...resultado, recargar };
}

/**
 * Hook para demorar (debounce) el valor de un input y no disparar
 * peticiones al servidor en cada tecla pulsada.
 */
export function useValorDemorado(valor, ms = 300) {
  const [demorado, setDemorado] = useState(valor);

  useEffect(() => {
    const timer = setTimeout(() => setDemorado(valor), ms);
    return () => clearTimeout(timer);
  }, [valor, ms]);

  return demorado;
}
