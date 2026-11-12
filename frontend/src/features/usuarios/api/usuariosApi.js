import axiosClient from "@/config/axiosClient";

export const RECURSOS = {
  SOCIO: "/socios",
  EMPLEADO: "/empleados",
  ADMINISTRADOR: "/administradores",
};

/**
 * CU03 — Búsqueda y listado paginado de personas según el rol del recurso.
 *
 * @param {"SOCIO" | "EMPLEADO" | "ADMINISTRADOR"} tipo
 * @param {{ dni?: string, nombre?: string, page?: number, size?: number }} filtros
 * @returns {Promise<{ contenido: Array, pagina: number, tamanio: number, totalElementos: number, totalPaginas: number }>}
 */
export async function listarUsuarios(tipo, { dni, nombre, page = 0, size = 20 } = {}) {
  const recurso = RECURSOS[tipo];
  if (!recurso) {
    throw new Error(`Tipo de usuario desconocido: ${tipo}`);
  }

  const params = { page, size };
  if (dni) params.dni = dni;
  if (nombre) params.nombre = nombre;

  const { data } = await axiosClient.get(recurso, { params });
  return data;
}

/**
 * CU03 — Consulta de datos detallados de una persona por su ID.
 *
 * @param {"SOCIO" | "EMPLEADO" | "ADMINISTRADOR"} tipo
 * @param {number|string} id
 */
export async function obtenerUsuario(tipo, id) {
  const recurso = RECURSOS[tipo];
  if (!recurso) {
    throw new Error(`Tipo de usuario desconocido: ${tipo}`);
  }

  const { data } = await axiosClient.get(`${recurso}/${id}`);
  return data;
}

/**
 * CU01 — Alta de una nueva persona en el recurso correspondiente.
 *
 * @param {"SOCIO" | "EMPLEADO" | "ADMINISTRADOR"} tipo
 * @param {Object} datos
 */
export async function crearUsuario(tipo, datos) {
  const recurso = RECURSOS[tipo];
  if (!recurso) {
    throw new Error(`Tipo de usuario desconocido: ${tipo}`);
  }

  const { data } = await axiosClient.post(recurso, datos);
  return data;
}

/**
 * CU02 — Modificación parcial (PATCH) de los datos de una persona.
 *
 * @param {"SOCIO" | "EMPLEADO" | "ADMINISTRADOR"} tipo
 * @param {number|string} id
 * @param {Object} datos
 */
export async function actualizarUsuario(tipo, id, datos) {
  const recurso = RECURSOS[tipo];
  if (!recurso) {
    throw new Error(`Tipo de usuario desconocido: ${tipo}`);
  }

  const { data } = await axiosClient.patch(`${recurso}/${id}`, datos);
  return data;
}

/**
 * CU04 — Eliminación física de la persona y revocación de sus sesiones.
 *
 * @param {"SOCIO" | "EMPLEADO" | "ADMINISTRADOR"} tipo
 * @param {number|string} id
 */
export async function eliminarUsuario(tipo, id) {
  const recurso = RECURSOS[tipo];
  if (!recurso) {
    throw new Error(`Tipo de usuario desconocido: ${tipo}`);
  }

  await axiosClient.delete(`${recurso}/${id}`);
}

/**
 * CU05 — Descarga de la ficha de la persona en formato PDF.
 * El endpoint del backend responde en /{id}/pdf con application/pdf.
 *
 * @param {"SOCIO" | "EMPLEADO" | "ADMINISTRADOR"} tipo
 * @param {number|string} id
 * @returns {Promise<Blob>}
 */
export async function imprimirFichaUsuario(tipo, id) {
  const recurso = RECURSOS[tipo];
  if (!recurso) {
    throw new Error(`Tipo de usuario desconocido: ${tipo}`);
  }

  const response = await axiosClient.get(`${recurso}/${id}/pdf`, {
    responseType: "blob",
  });
  return response.data;
}
