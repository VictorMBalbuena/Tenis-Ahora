import axiosClient from "@/config/axiosClient";

/** CU22: listado paginado, filtrable por texto, estado de validación y activo. */
export async function listarProfesores({ busqueda = "", estado = "", activo = "", page = 0, size = 20 } = {}) {
  const params = { page, size };
  if (busqueda.trim()) params.nombre = busqueda.trim();
  if (estado) params.estado = estado;
  if (activo !== "") params.activo = activo;

  const { data } = await axiosClient.get("/profesores", { params });
  return data;
}

/** CU22: detalle completo de un profesor. */
export async function obtenerProfesor(id) {
  const { data } = await axiosClient.get(`/profesores/${id}`);
  return data;
}

/** CU20: alta, con título habilitante y certificación. */
export async function crearProfesor(datos) {
  const { data } = await axiosClient.post("/profesores", datos);
  return data;
}

/** CU21: modificación parcial. El estado de validación no se toca por acá. */
export async function actualizarProfesor(id, datos) {
  const { data } = await axiosClient.patch(`/profesores/${id}`, datos);
  return data;
}

/** CU23: baja. El backend la rechaza si tiene historial (clases, reservas) asociado. */
export async function eliminarProfesor(id) {
  await axiosClient.delete(`/profesores/${id}`);
}

/** CU31: aprueba o rechaza (con motivo) el título habilitante. Solo administrador. */
export async function decidirCertificacionProfesor(id, { aprobada, motivo = null }) {
  const { data } = await axiosClient.post(`/profesores/${id}/validacion`, { aprobada, motivo });
  return data;
}

/** CU24: ficha en PDF, generada por el backend. */
export async function imprimirProfesor(id) {
  const { data } = await axiosClient.get(`/profesores/${id}/pdf`, { responseType: "blob" });
  return data;
}
