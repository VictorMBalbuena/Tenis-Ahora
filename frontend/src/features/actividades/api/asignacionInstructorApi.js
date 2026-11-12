import axiosClient from "@/config/axiosClient";

/** CU44: asignar un profesor o un entrenador a una clase o entrenamiento particular; cuelga de /actividades porque sirve a las dos. */

const RECURSO = "/actividades";

/** Profesores o entrenadores con certificación validada y sin otra actividad que se pise con este horario. */
export async function listarInstructoresDisponibles(actividadId) {
  const { data } = await axiosClient.get(`${RECURSO}/${actividadId}/instructores-disponibles`);
  return data;
}

/** Reemplaza el instructor de la actividad particular. */
export async function asignarInstructor(actividadId, { rol, instructorId }) {
  await axiosClient.put(`${RECURSO}/${actividadId}/instructor`, { rol, instructorId });
}
