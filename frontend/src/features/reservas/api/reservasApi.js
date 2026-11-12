import axiosClient from "@/config/axiosClient";

const RECURSO = "/reservas";

let reglasEnCurso = null;

/**
 * Reglas vigentes de las reservas (antelación, duración, seña, plazos). Se piden una vez por sesión de la página.
 */
export function obtenerReglasDeReserva() {
  if (!reglasEnCurso) {
    reglasEnCurso = axiosClient.get(`${RECURSO}/reglas`).then(({ data }) => data).catch((error) => {
      reglasEnCurso = null;
      throw error;
    });
  }
  return reglasEnCurso;
}

/**
 * Busca un socio activo por DNI exacto para sumarlo como acompañante.
 */
export async function buscarJugadorPorDni(dni) {
  const { data } = await axiosClient.get(`${RECURSO}/jugadores`, { params: { dni } });
  return data;
}

/**
 * Consulta la grilla de disponibilidad de canchas por fecha y rango opcional (CU19).
 */
export async function consultarDisponibilidad(params) {
  const { data } = await axiosClient.get(`${RECURSO}/disponibilidad`, { params });
  return data;
}

/**
 * Consulta los insumos disponibles para un turno específico (CU13).
 */
export async function consultarInsumosDisponibles(params) {
  const { data } = await axiosClient.get(`${RECURSO}/insumos-disponibles`, { params });
  return data;
}

/**
 * Registra una nueva reserva de cancha (CU16).
 */
export async function crearReserva(payload) {
  const { data } = await axiosClient.post(RECURSO, payload);
  return data;
}

/**
 * Consulta las reservas del socio autenticado (próximas o pasadas).
 */
export async function obtenerMisReservas(params) {
  const { data } = await axiosClient.get(`${RECURSO}/mis-reservas`, { params });
  return data;
}

/**
 * Búsqueda paginada de reservas para personal administrativo o empleados (CU17).
 */
export async function buscarReservas(params) {
  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/**
 * Obtiene el detalle de una reserva por su identificador único.
 */
export async function obtenerReserva(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/**
 * Modifica parcialmente una reserva existente (CU17).
 */
export async function modificarReserva(id, payload) {
  const { data } = await axiosClient.patch(`${RECURSO}/${id}`, payload);
  return data;
}

/**
 * Previsualiza las condiciones y reembolso de cancelación de una reserva (CU18).
 */
export async function previsualizarCancelacion(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/cancelacion`);
  return data;
}

/**
 * Confirma la cancelación de una reserva liberando cancha e insumos (CU18).
 */
export async function confirmarCancelacion(id) {
  const { data } = await axiosClient.post(`${RECURSO}/${id}/cancelacion`);
  return data;
}

/**
 * Inicia el alquiler de una reserva confirmada (CU14).
 * Pasa la reserva a EN_CURSO y descuenta los insumos asignados del stock.
 */
export async function iniciarAlquiler(id) {
  const { data } = await axiosClient.post(`${RECURSO}/${id}/inicio`);
  return data;
}

/**
 * Finaliza el alquiler de una reserva en curso (CU15).
 * Pasa la reserva a FINALIZADA y repone los insumos descontados al stock.
 */
export async function finalizarAlquiler(id) {
  const { data } = await axiosClient.post(`${RECURSO}/${id}/finalizacion`);
  return data;
}
