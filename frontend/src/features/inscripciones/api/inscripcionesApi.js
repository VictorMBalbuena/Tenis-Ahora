import axiosClient from "@/config/axiosClient";

// Inscripciones a clases y entrenamientos vistas desde quien las paga: estado, seña, saldo e historial.
const RECURSO = "/actividades/inscripciones";

export async function listarMisInscripciones() {
  const { data } = await axiosClient.get(`${RECURSO}/mias`);
  return data;
}

export async function obtenerInscripcion(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

export async function previsualizarBaja(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/baja`);
  return data;
}
