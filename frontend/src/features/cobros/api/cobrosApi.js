import axiosClient from "@/config/axiosClient";

// Modulo Cobros: cobrar la sena de una reserva, consultar, listar, anular, imprimir y ver el recibo.

const RECURSO = "/cobros";

/** CU54: listado paginado. Filtra por codigo de cobro o numero de reserva (no por torneo ni numero de recibo). */
export async function listarCobros({
  busqueda = "",
  concepto = "",
  medioPago = "",
  desde = "",
  hasta = "",
  page = 0,
  size = 20,
} = {}) {
  const params = { page, size };
  if (busqueda.trim()) params.busqueda = busqueda.trim();
  if (concepto) params.concepto = concepto;
  if (medioPago) params.medioPago = medioPago;
  if (desde) params.desde = desde;
  if (hasta) params.hasta = hasta;

  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/** El registro de cobros del socio que inicio sesion. */
export async function listarMisCobros({ page = 0, size = 20 } = {}) {
  const { data } = await axiosClient.get(`${RECURSO}/mios`, { params: { page, size } });
  return data;
}

/** CU54: detalle de un cobro, con su reserva o torneo, su recibo y sus descuentos. */
export async function obtenerCobro(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/**
 * CU51: cobrar la sena de una reserva puntual.
 *
 * El monto y los descuentos los calcula siempre el servidor: el pedido solo
 * lleva el medio de pago.
 */
export async function cobrarSeniaDeReserva(reservaId, { medioPago, numeroOperacion, cuotas, aprobado } = {}) {
  const { data } = await axiosClient.post(`/reservas/${reservaId}/cobros`, {
    medioPago,
    numeroOperacion,
    cuotas,
    aprobado,
  });
  return data;
}

/** CU55: anula el cobro con su motivo; nunca se borra. Solo lo puede hacer el administrador. */
export async function anularCobro(id, motivo) {
  const { data } = await axiosClient.post(`${RECURSO}/${id}/anulacion`, { motivo });
  return data;
}

/** CU56: comprobante del cobro en PDF, generado por el backend. */
export async function imprimirCobro(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/pdf`, { responseType: "blob" });
  return data;
}

/**
 * CU57: ver el recibo de un cobro.
 *
 * El backend emite el recibo automaticamente al acreditarse el pago: no hay
 * una accion para emitirlo a mano, asi que esto consulta el que ya existe
 * (GET /cobros/{id}/recibo) en vez de crear uno.
 */
export async function emitirRecibo(cobroId) {
  const { data } = await axiosClient.get(`${RECURSO}/${cobroId}/recibo`);
  return data;
}

/** Reservas con saldo pendiente, para elegir a cual cobrarle (CU51): son las que estan PENDIENTE_PAGO. */
export async function listarReservasConSaldo() {
  const { data } = await axiosClient.get("/reservas", { params: { estado: "PENDIENTE_PAGO", size: 50 } });
  return data.contenido || [];
}
