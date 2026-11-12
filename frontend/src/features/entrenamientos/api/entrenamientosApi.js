import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";

// Módulo Entrenamientos (CU37-CU41, CU82-CU84): EntrenamientoController no filtra por texto libre ni rango de fechas, sino por cancha, entrenador, una fecha puntual, día de la semana y estado.

const RECURSO = "/entrenamientos";

/** CU83/CU84: listado de mis entrenamientos como entrenador autenticado (el backend limita size a 50 máx). */
export async function listarMisEntrenamientos({ page = 0, size = 50 } = {}) {
  const safeSize = Math.min(Math.max(1, Number(size) || 50), 50);
  const { data } = await axiosClient.get(`${RECURSO}/mis-entrenamientos`, {
    params: { page, size: safeSize },
  });
  return data;
}

/** CU39 / CU84: listado paginado. Si el rol es ENTRENADOR, consulta /mis-entrenamientos automáticamente. */
export async function listarEntrenamientos({
  canchaId = "",
  entrenadorId = "",
  fecha = "",
  dia = "",
  estado = "",
  page = 0,
  size = 20,
} = {}) {
  const rol = useAuthStore.getState().user?.rol;
  if (rol === "ENTRENADOR") {
    return listarMisEntrenamientos({ page, size });
  }

  const params = { page, size };
  if (canchaId) params.canchaId = canchaId;
  if (entrenadorId) params.entrenadorId = entrenadorId;
  if (fecha) params.fecha = fecha;
  if (dia) params.dia = dia;
  if (estado) params.estado = estado;

  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/** CU39: detalle, con su entrenador, su cancha y el cupo ya calculado. */
export async function obtenerEntrenamiento(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/** CU37: programar un entrenamiento. */
export async function crearEntrenamiento(datos) {
  const { data } = await axiosClient.post(RECURSO, datos);
  return data;
}

/** CU38: modificación parcial. Solo viajan los campos que cambiaron. */
export async function actualizarEntrenamiento(id, datos) {
  const { data } = await axiosClient.patch(`${RECURSO}/${id}`, datos);
  return data;
}

/** CU40: baja. El backend la rechaza si ya tiene alumnos inscriptos. */
export async function eliminarEntrenamiento(id) {
  await axiosClient.delete(`${RECURSO}/${id}`);
}

/** CU41: ficha del entrenamiento en PDF, generada por el backend. */
export async function imprimirEntrenamiento(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/pdf`, { responseType: "blob" });
  return data;
}

/** CU83/CU84: alumnos inscriptos al entrenamiento (no paginado). */
export async function listarInscriptos(entrenamientoId) {
  const { data } = await axiosClient.get(`${RECURSO}/${entrenamientoId}/inscriptos`);
  return data;
}

/** CU82: inscribir un alumno. Si el cupo está completo el backend lo rechaza. */
export async function inscribirAlumno(entrenamientoId, socioId) {
  const { data } = await axiosClient.post(`${RECURSO}/${entrenamientoId}/inscripciones`, { socioId });
  return data;
}

/** CU82: cancelar la inscripción de un socio, para liberar el lugar. */
export async function desinscribirAlumno(entrenamientoId, socioId) {
  await axiosClient.delete(`${RECURSO}/${entrenamientoId}/inscripciones/${socioId}`);
}

/** CU80: el socio se inscribe a sí mismo; el backend toma el socio de la sesión. */
export async function inscribirme(id) {
  const { data } = await axiosClient.post(`${RECURSO}/${id}/inscripciones`);
  return data;
}
