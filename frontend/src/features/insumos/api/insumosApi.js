import axiosClient from "@/config/axiosClient";

/**
 * Capa de acceso al módulo Insumos del backend (BACK 05).
 *
 * Todos los endpoints exigen rol EMPLEADO o ADMINISTRADOR: el
 * @PreAuthorize está en InsumoController y en MovimientoStockController.
 * El token lo inyecta el interceptor de axiosClient, acá no se toca.
 */

const RECURSO = "/insumos";
const RECURSO_CATEGORIAS = "/categorias-insumo";

/**
 * CU102 — Listado paginado de insumos con filtros.
 *
 * @param {{ busqueda?: string, categoriaId?: number|string, estado?: string, page?: number, size?: number }} filtros
 * @returns {Promise<{ contenido: Array, pagina: number, tamanio: number, totalElementos: number, totalPaginas: number }>}
 */
export async function listarInsumos({
  busqueda,
  categoriaId,
  estado,
  page = 0,
  size = 20,
} = {}) {
  const params = { page, size };
  if (busqueda) params.busqueda = busqueda;
  if (categoriaId) params.categoriaId = categoriaId;
  if (estado) params.estado = estado;

  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/**
 * Totales para la tira de KPIs del listado, sobre todos los insumos activos
 * del club (no solo los de la página actual).
 *
 * @returns {Promise<{ distintos: number, enDeposito: number, asignadas: number, alertas: number }>}
 */
export async function obtenerResumenInsumos() {
  const { data } = await axiosClient.get(`${RECURSO}/resumen`);
  return data;
}

/** Categorías de insumo (Raqueta, Pelota, Red, ...): editables, no son un enum fijo. */
export async function listarCategoriasInsumo() {
  const { data } = await axiosClient.get(RECURSO_CATEGORIAS);
  return data;
}

/**
 * Da de alta una categoría de insumo nueva.
 *
 * @param {string} nombre
 */
export async function crearCategoriaInsumo(nombre) {
  const { data } = await axiosClient.post(RECURSO_CATEGORIAS, { nombre });
  return data;
}

/**
 * Le cambia el nombre a una categoría existente. No afecta a los insumos
 * que ya la tienen asignada.
 *
 * @param {number|string} id
 * @param {string} nombre
 */
export async function actualizarCategoriaInsumo(id, nombre) {
  const { data } = await axiosClient.patch(`${RECURSO_CATEGORIAS}/${id}`, { nombre });
  return data;
}

/**
 * Elimina una categoría. El backend rechaza el borrado (409) si hay
 * insumos —activos o dados de baja— que todavía la tienen asignada.
 *
 * @param {number|string} id
 */
export async function eliminarCategoriaInsumo(id) {
  await axiosClient.delete(`${RECURSO_CATEGORIAS}/${id}`);
}

/**
 * CU11 — Consulta de un insumo por su ID.
 *
 * @param {number|string} id
 */
export async function obtenerInsumo(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/**
 * CU10 — Alta de un insumo. Nace activo y con el stock inicial indicado.
 *
 * @param {{ nombre: string, categoriaId: number, cantidadDisponible: number, cantidadMinima: number }} datos
 */
export async function crearInsumo(datos) {
  const { data } = await axiosClient.post(RECURSO, datos);
  return data;
}

/**
 * CU12 — Modificación parcial (PATCH). Solo viajan los campos presentes.
 *
 * Si se manda cantidadDisponible, el backend lo registra como un ajuste en
 * el historial de movimientos; no es una corrección silenciosa.
 *
 * @param {number|string} id
 * @param {{ nombre?: string, categoriaId?: number, cantidadMinima?: number, cantidadDisponible?: number }} datos
 */
export async function actualizarInsumo(id, datos) {
  const { data } = await axiosClient.patch(`${RECURSO}/${id}`, datos);
  return data;
}

/**
 * CU101 — Baja del insumo.
 *
 * @param {number|string} id
 */
export async function eliminarInsumo(id) {
  await axiosClient.delete(`${RECURSO}/${id}`);
}

/**
 * CU14 y CU15 — Descontar o reponer stock.
 *
 * Devuelve el insumo ya actualizado, así la pantalla no necesita volver a
 * pedirlo. Si la cantidad a descontar supera lo disponible, el backend
 * responde con error y el stock no cambia.
 *
 * @param {number|string} insumoId
 * @param {{ tipo: "DESCUENTO" | "REPOSICION", cantidad: number, motivo?: string }} movimiento
 */
export async function registrarMovimiento(insumoId, { tipo, cantidad, motivo }) {
  const { data } = await axiosClient.post(`${RECURSO}/${insumoId}/movimientos`, {
    tipo,
    cantidad,
    motivo,
  });
  return data;
}

/**
 * Historial paginado de movimientos de un insumo.
 *
 * Va aparte del detalle porque en el backend es un sub-recurso con su propia
 * paginación: GET /api/insumos/{id}/movimientos
 *
 * @param {number|string} insumoId
 * @param {{ page?: number, size?: number }} paginado
 */
export async function listarMovimientos(insumoId, { page = 0, size = 20 } = {}) {
  const { data } = await axiosClient.get(
    `${RECURSO}/${insumoId}/movimientos`,
    { params: { page, size } },
  );
  return data;
}
