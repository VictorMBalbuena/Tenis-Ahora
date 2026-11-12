import axiosClient from "@/config/axiosClient";

const RECURSO = "/tipos-cancha";

/**
 * Listado de tipos de cancha activos con su superficie y capacidad.
 *
 * @returns {Promise<Array<{ id: number, nombre: string, tipoSuperficie: string, capacidad: number, descripcion: string, activo: boolean, cantidadCanchas: number }>>}
 */
export async function listarTiposCancha() {
  const { data } = await axiosClient.get(RECURSO);
  return data;
}

/**
 * Consulta de un tipo de cancha por ID.
 */
export async function obtenerTipoCancha(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/**
 * Registra un nuevo tipo de cancha.
 *
 * @param {{ nombre: string, tipoSuperficie: string, capacidad: number, descripcion?: string }} datos
 */
export async function crearTipoCancha(datos) {
  const { data } = await axiosClient.post(RECURSO, datos);
  return data;
}

/**
 * Modifica un tipo de cancha existente.
 *
 * @param {number|string} id
 * @param {{ nombre: string, tipoSuperficie: string, capacidad: number, descripcion?: string }} datos
 */
export async function actualizarTipoCancha(id, datos) {
  const { data } = await axiosClient.put(`${RECURSO}/${id}`, datos);
  return data;
}

/**
 * Da de baja lógica un tipo de cancha.
 * Falla con 409 si tiene canchas activas asociadas.
 *
 * @param {number|string} id
 */
export async function eliminarTipoCancha(id) {
  await axiosClient.delete(`${RECURSO}/${id}`);
}

/**
 * Descarga el reporte PDF de tipos de cancha y sus características.
 *
 * @returns {Promise<Blob>}
 */
export async function descargarReportePdf() {
  const { data } = await axiosClient.get(`${RECURSO}/reporte`, {
    responseType: "blob",
  });
  return data;
}

