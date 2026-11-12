import axiosClient from "@/config/axiosClient";

/**
 * Modulo Recibos de pago (CU57, CU59-CU61).
 *
 * El recibo es un comprobante, no se edita (CU58 no es un caso de uso real:
 * un recibo emitido no se modifica). Se emite solo al acreditarse el cobro y
 * todavia no se puede anular por separado. Estas rutas son solo para el personal.
 */

const RECURSO = "/recibos";

/** CU59: listado paginado. Filtra por numero de recibo (el id del cobro que lo origino) y rango de fecha de emision. */
export async function listarRecibos({ busqueda = "", desde = "", hasta = "", page = 0, size = 20 } = {}) {
  const params = { page, size };
  if (busqueda.trim()) params.busqueda = busqueda.trim();
  if (desde) params.desde = desde;
  if (hasta) params.hasta = hasta;

  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/** CU59: detalle de un recibo, con el cobro que lo origino. */
export async function obtenerRecibo(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/** CU60: sin soporte en el backend (no existe DELETE /recibos/{id}). */
export async function anularRecibo() {
  throw new Error("El backend no permite anular un recibo (CU60).");
}

/** CU61: recibo en PDF, generado por el backend. */
export async function imprimirRecibo(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/pdf`, { responseType: "blob" });
  return data;
}
