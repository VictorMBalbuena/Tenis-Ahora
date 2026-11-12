import axiosClient from "@/config/axiosClient";

const RECURSO = "/torneos";

export async function listarTorneos() {
  const { data } = await axiosClient.get(RECURSO);
  return data;
}

export async function obtenerTorneo(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

export async function crearTorneo(datos) {
  const { data } = await axiosClient.post(RECURSO, datos);
  return data;
}

export async function modificarTorneo(id, datos) {
  const { data } = await axiosClient.put(`${RECURSO}/${id}`, datos);
  return data;
}

export async function eliminarTorneo(id) {
  await axiosClient.delete(`${RECURSO}/${id}`);
}

export async function listarInscripciones(torneoId) {
  const { data } = await axiosClient.get(`${RECURSO}/${torneoId}/inscripciones`);
  return data;
}

export async function inscribirTorneo(torneoId, datos) {
  const { data } = await axiosClient.post(`${RECURSO}/${torneoId}/inscripciones`, datos);
  return data;
}

export async function generarFixture(torneoId) {
  const { data } = await axiosClient.post(`${RECURSO}/${torneoId}/fixture`);
  return data;
}

export async function obtenerPartidos(torneoId) {
  const { data } = await axiosClient.get(`${RECURSO}/${torneoId}/partidos`);
  return data;
}

export async function cargarResultado(torneoId, partidoId, sets) {
  const { data } = await axiosClient.post(`${RECURSO}/${torneoId}/partidos/${partidoId}/resultado`, {
    sets,
  });
  return data;
}

/**
 * La tabla de posiciones de una liga. Solo tiene sentido en una liga: un torneo por eliminación se sigue por su
 * cuadro, y el backend responde 400 si se le pide la tabla de un torneo.
 */
export async function obtenerPosiciones(torneoId) {
  const { data } = await axiosClient.get(`${RECURSO}/${torneoId}/posiciones`);
  return data;
}

export async function descargarPdfResultados(torneoId) {
  const { data } = await axiosClient.get(`${RECURSO}/${torneoId}/resultados/pdf`, {
    responseType: "blob",
  });
  return data;
}

export async function listarMisInscripcionesATorneos() {
  const { data } = await axiosClient.get(`${RECURSO}/inscripciones/mias`);
  return data;
}

/**
 * CU47: busca al compañero de pareja por su DNI exacto.
 *
 * Se busca por DNI y no por id porque el id es interno: nadie sabe el número de otro socio.
 */
export async function buscarCompaneroPorDni(dni) {
  const { data } = await axiosClient.get(`${RECURSO}/companeros`, { params: { dni } });
  return data;
}
