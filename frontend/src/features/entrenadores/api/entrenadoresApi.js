import axiosClient from "@/config/axiosClient";

/** CU27: listado paginado, filtrable por texto, especialidad, estado de certificación y activo. */
export async function listarEntrenadores({ busqueda = "", especialidad = "", estado = "", activo = "", page = 0, size = 20 } = {}) {
  const params = { page, size };
  const texto = busqueda.trim();
  if (texto) {
    params.nombre = texto;
  }
  if (especialidad) params.especialidad = especialidad;
  if (estado) params.estado = estado;
  if (activo !== "") params.activo = activo;

  const { data } = await axiosClient.get("/entrenadores", { params });
  return data;
}

/** CU27: detalle completo de un entrenador. */
export async function obtenerEntrenador(id) {
  const { data } = await axiosClient.get(`/entrenadores/${id}`);
  return data;
}

/** CU25: alta, con certificación deportiva y certificación. */
export async function crearEntrenador(datos) {
  const { data } = await axiosClient.post("/entrenadores", datos);
  return data;
}

/** CU26: modificación parcial. El estado de validación no se toca por acá. */
export async function actualizarEntrenador(id, datos) {
  const { data } = await axiosClient.patch(`/entrenadores/${id}`, datos);
  return data;
}

/** CU28: baja. El backend la rechaza si tiene historial (entrenamientos) asociado. */
export async function eliminarEntrenador(id) {
  await axiosClient.delete(`/entrenadores/${id}`);
}

/** CU31: aprueba o rechaza (con motivo) la certificación. Solo administrador. */
export async function decidirCertificacionEntrenador(id, { aprobada, motivo = null }) {
  const { data } = await axiosClient.post(`/entrenadores/${id}/validacion`, { aprobada, motivo });
  return data;
}

/** CU29: ficha en PDF, generada por el backend. */
export async function imprimirEntrenador(id) {
  const { data } = await axiosClient.get(`/entrenadores/${id}/pdf`, { responseType: "blob" });
  return data;
}
