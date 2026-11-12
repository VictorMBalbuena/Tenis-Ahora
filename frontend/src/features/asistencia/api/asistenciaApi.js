import axiosClient from "@/config/axiosClient";

/** CU42, CU43: GET y POST a /actividades/{id}/asistencias, cuelga de Actividad porque sirve tanto a Clase como a Entrenamiento. */

const RECURSO = "/actividades";

/** Planilla de una fecha: todos los inscriptos con su marca (ausente si todavía no se registró). */
export async function obtenerPlanilla(actividadId, fecha) {
  const { data } = await axiosClient.get(`${RECURSO}/${actividadId}/asistencias`, { params: { fecha } });
  return data;
}

/**
 * Guarda la planilla completa de esa fecha.
 *
 * Va entera y no marca por marca: si se guardara de a uno, una conexion
 * cortada a la mitad dejaria media planilla registrada.
 */
export async function guardarPlanilla(actividadId, { fecha, marcas }) {
  const { data } = await axiosClient.post(`${RECURSO}/${actividadId}/asistencias`, { fecha, marcas });
  return data;
}
