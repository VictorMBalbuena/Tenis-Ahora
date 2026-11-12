import axiosClient from "@/config/axiosClient";
import { listarUsuarios } from "@/features/usuarios/api/usuariosApi";
import { listarProfesores } from "@/features/profesores/api/profesoresApi";
import { listarEntrenadores } from "@/features/entrenadores/api/entrenadoresApi";
import { listarClases } from "@/features/clases/api/clasesApi";
import { listarEntrenamientos } from "@/features/entrenamientos/api/entrenamientosApi";
import { listarCobros } from "@/features/cobros/api/cobrosApi";
import { listarRecibos } from "@/features/recibos/api/recibosApi";
import { listarDescuentos } from "@/features/descuentos/api/descuentosApi";
import { listarTorneos, obtenerPartidos } from "@/features/torneos/api/torneosApi";

/**
 * De dónde salen los datos de cada listado imprimible.
 *
 * No hay endpoints de lista en PDF en el backend: los `/pdf` que existen son
 * de una entidad a la vez (una clase, un profesor, un cobro). Así que el
 * listado se arma acá con los mismos endpoints que ya usan las pantallas y se
 * imprime desde el navegador.
 *
 * Cada función devuelve los datos crudos del servidor. Pasarlos a filas es
 * trabajo de `model/listados.js`, que no habla con la red y por eso se puede
 * probar sola.
 */

/** Lo máximo que acepta el backend en un pedido: pedir más devuelve 400. */
export const TAMANIO_DE_PAGINA = 50;

/**
 * Hasta dónde se recorre un listado.
 *
 * Un listado se imprime entero, pero el backend pagina todo, así que hay que
 * ir pidiendo página por página. Esto le pone un techo a esa vuelta para que
 * un módulo con miles de filas no cuelgue la pantalla ni gaste una resma. El
 * pie del impreso avisa cuando se llegó al techo.
 */
export const TOPE_DE_FILAS = 500;

/** Las respuestas paginadas del backend vienen como { contenido, totalPaginas, ... }. */
function contenidoDe(respuesta) {
  if (Array.isArray(respuesta)) return respuesta;
  return respuesta?.contenido ?? [];
}

/**
 * Recorre todas las páginas de un listado y las junta en un solo array.
 *
 * Corta cuando no quedan páginas, cuando se llegó al tope o cuando una página
 * vuelve vacía: eso último es por las dudas, para no quedar dando vueltas si
 * el servidor informara mal el total.
 */
async function todasLasPaginas(pedirPagina) {
  const filas = [];
  let page = 0;
  let totalPaginas = 1;

  while (page < totalPaginas && filas.length < TOPE_DE_FILAS) {
    const respuesta = await pedirPagina({ page, size: TAMANIO_DE_PAGINA });
    const contenido = contenidoDe(respuesta);
    if (contenido.length === 0) break;

    filas.push(...contenido);
    totalPaginas = respuesta?.totalPaginas ?? 1;
    page += 1;
  }

  return filas.slice(0, TOPE_DE_FILAS);
}

/** Las tres tablas de personas que no son instructores (CU03). */
async function traerUsuarios() {
  const [socios, empleados, administradores] = await Promise.all([
    todasLasPaginas((p) => listarUsuarios("SOCIO", p)),
    todasLasPaginas((p) => listarUsuarios("EMPLEADO", p)),
    todasLasPaginas((p) => listarUsuarios("ADMINISTRADOR", p)),
  ]);
  return [...socios, ...empleados, ...administradores];
}

/**
 * Canchas: es el único listado sin módulo de API propio en el front.
 * `useCanchas` llama a axios directamente, así que acá se hace lo mismo.
 */
function traerCanchas() {
  return todasLasPaginas(async (params) => {
    const { data } = await axiosClient.get("/canchas", { params });
    return data;
  });
}

/** Todos los partidos de todos los torneos y ligas, con el nombre de su competencia. */
async function traerPartidos() {
  // El listado de torneos no pagina: devuelve un array.
  const torneos = contenidoDe(await listarTorneos());
  const porTorneo = await Promise.all(
    torneos.map(async (torneo) => {
      const partidos = contenidoDe(await obtenerPartidos(torneo.id));
      return partidos.map((partido) => ({ ...partido, torneo }));
    }),
  );
  return porTorneo.flat().slice(0, TOPE_DE_FILAS);
}

const FUENTES = {
  usuarios: traerUsuarios,
  canchas: traerCanchas,
  profesores: () => todasLasPaginas(listarProfesores),
  entrenadores: () => todasLasPaginas(listarEntrenadores),
  clases: () => todasLasPaginas(listarClases),
  entrenamientos: () => todasLasPaginas(listarEntrenamientos),
  partidos: traerPartidos,
  cobros: () => todasLasPaginas(listarCobros),
  recibos: () => todasLasPaginas(listarRecibos),
  descuentos: () => todasLasPaginas(listarDescuentos),
};

/** Los datos crudos de un listado, por su id. */
export async function traerDatos(id) {
  const fuente = FUENTES[id];
  if (!fuente) {
    throw new Error(`Listado desconocido: ${id}`);
  }
  return fuente();
}
