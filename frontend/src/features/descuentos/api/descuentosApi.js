import axiosClient from "@/config/axiosClient";

/**
 * Única capa de la feature que habla con el servidor.
 *
 * Responde contra el backend real (`/api/descuentos`, solo administradores).
 * Antes respondía contra un mock en memoria porque BACK 16 no existía; ahora
 * existe y el contrato coincide, así que esto es solo la llamada.
 *
 * Firma que espera el front:
 *   listarDescuentos({ busqueda, tipo, activo, page, size }) -> PageResponse  CU64
 *   obtenerDescuento(id)                                     -> Descuento     CU64
 *   crearDescuento(datos)                                    -> Descuento     CU62
 *   actualizarDescuento(id, datos)                           -> Descuento     CU63
 *   eliminarDescuento(id)                                    -> void          CU65
 *   validarCodigo(codigo, monto)                             -> CodigoValidado RF69
 */

const RECURSO = "/descuentos";

export const CodigoError = {
  DESCUENTO_DUPLICADO: "DESCUENTO_DUPLICADO",
  BENEFICIO_INVALIDO: "BENEFICIO_INVALIDO",
  NO_ENCONTRADO: "NO_ENCONTRADO",
  EN_USO: "EN_USO",
};

/** Lo que viaja al servidor. El id y `vecesAplicado` los pone él, no se mandan. */
function cuerpo(datos) {
  return {
    codigo: datos.codigo,
    tipo: datos.tipo,
    porcentaje: datos.porcentaje ?? null,
    montoFijo: datos.montoFijo ?? null,
    condiciones: datos.condiciones ?? null,
    activo: datos.activo ?? true,
  };
}

/** CU64: listado paginado con los filtros de la pantalla. */
export async function listarDescuentos({ busqueda = "", tipo = "", activo = "", page = 0, size = 20 } = {}) {
  const params = { page, size };
  if (busqueda.trim()) params.busqueda = busqueda.trim();
  if (tipo) params.tipo = tipo;
  // La pantalla usa "SI"/"NO"; la API espera un booleano.
  if (activo !== "") params.activo = activo === "SI";

  const { data } = await axiosClient.get(RECURSO, { params });
  return data;
}

/** CU64: el detalle de un descuento, con cuántas veces se aplicó. */
export async function obtenerDescuento(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}`);
  return data;
}

/** CU62: alta. El código es obligatorio y queda guardado en mayúsculas. */
export async function crearDescuento(datos) {
  const { data } = await axiosClient.post(RECURSO, cuerpo(datos));
  return data;
}

/** CU63: modificación. */
export async function actualizarDescuento(id, datos) {
  const { data } = await axiosClient.put(`${RECURSO}/${id}`, cuerpo(datos));
  return data;
}

/** CU65: baja. El servidor la rechaza si el descuento ya se aplicó a algún cobro. */
export async function eliminarDescuento(id) {
  await axiosClient.delete(`${RECURSO}/${id}`);
}

/** CU66: la ficha en PDF. */
export async function imprimirDescuento(id) {
  const { data } = await axiosClient.get(`${RECURSO}/${id}/pdf`, { responseType: "blob" });
  return data;
}

/**
 * RF69: valida un código al reservar y dice cuánto descontaría sobre ese monto.
 *
 * Es una vista previa: el monto que se termina cobrando lo vuelve a calcular el
 * servidor al guardar la reserva, con el total real.
 */
export async function validarCodigo(codigo, monto = 0) {
  const { data } = await axiosClient.get(`${RECURSO}/codigo/${encodeURIComponent(codigo.trim())}`, {
    params: { monto },
  });
  return data;
}
