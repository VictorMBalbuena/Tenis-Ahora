import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";

/** Módulo Clases (CU32-CU36, CU70, CU80-CU82): sin búsqueda por texto ni rango desde/hasta, el backend solo filtra por cancha, profesor, fecha puntual, día de la semana, nivel y estado. */

const RECURSO = "/clases";

/** CU81/CU82: listado de mis clases como profesor autenticado (el backend limita size a 50 máx). */
export async function listarMisClases({ page = 0, size = 50 } = {}) {
  const safeSize = Math.min(Math.max(1, Number(size) || 50), 50);
  const { data } = await axiosClient.get(`${RECURSO}/mis-clases`, {
    params: { page, size: safeSize },
  });
  return data;
}

/** CU34: listado paginado, filtrable por cancha, profesor, una fecha dentro del rango, día de la semana, nivel y estado. Si el rol es PROFESOR, delega a mis-clases. */
export async function listarClases({
  canchaId = "",
  profesorId = "",
  fecha = "",
  dia = "",
  nivel = "",
  estado = "",
  page = 0,
  size = 20,
  esProfesor = false,
} = {}) {
  const rol = useAuthStore.getState().user?.rol;
  if (esProfesor || rol === "PROFESOR") {
    return listarMisClases({ page, size });
  }

  const params = { page, size };
  if (canchaId) params.canchaId = canchaId;
  if (profesorId) params.profesorId = profesorId;
  if (fecha) params.fecha = fecha;
  if (dia) params.dia = dia;
  if (nivel) params.nivel = nivel;
  if (estado) params.estado = estado;

  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/** CU34: detalle de una clase, con su profesor, su cancha y el cupo ya calculado por el backend. */
export async function obtenerClase(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/** CU32 y CU70: programar una clase recurrente (rango de fechas + días de la semana + horario fijo). */
export async function crearClase(datos) {
  const { data } = await axiosClient.post(RECURSO, datos);
  return data;
}

/** CU33: modificación parcial. Solo viajan los campos que cambiaron. */
export async function actualizarClase(id, datos) {
  const { data } = await axiosClient.patch(`${RECURSO}/${id}`, datos);
  return data;
}

/** CU35: baja. El backend la rechaza si ya tiene alumnos inscriptos. */
export async function eliminarClase(id) {
  await axiosClient.delete(`${RECURSO}/${id}`);
}

/** CU36: ficha de la clase en PDF, generada por el backend. */
export async function imprimirClase(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/pdf`, { responseType: "blob" });
  return data;
}

/** CU81 y CU82: alumnos inscriptos a la clase (lista plana, sin paginar). */
export async function listarInscriptos(claseId) {
  const { data } = await axiosClient.get(`${RECURSO}/${claseId}/inscriptos`);
  return data;
}

/**
 * CU80: inscribir un alumno.
 *
 * Si la clase está completa el backend responde con error y la inscripción no
 * se registra; la pantalla además lo impide antes de llegar hasta acá.
 */
export async function inscribirAlumno(claseId, socioId) {
  const { data } = await axiosClient.post(`${RECURSO}/${claseId}/inscripciones`, { socioId });
  return data;
}

/** CU80: dar de baja la inscripción de un alumno, para liberar el lugar. */
export async function desinscribirAlumno(claseId, socioId) {
  await axiosClient.delete(`${RECURSO}/${claseId}/inscripciones/${socioId}`);
}

/** CU80: el socio se inscribe a sí mismo; el backend toma el socio de la sesión. */
export async function inscribirme(id) {
  const { data } = await axiosClient.post(`${RECURSO}/${id}/inscripciones`);
  return data;
}
