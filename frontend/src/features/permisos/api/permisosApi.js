import axiosClient from "@/config/axiosClient";

/**
 * Única capa de la feature que habla con el servidor.
 *
 * Módulo de roles y permisos: CU74 (asignar rol), CU75 (configurar permisos por
 * rol), CU76 (habilitar o deshabilitar cuentas) y CU85 (crear rol).
 *
 * Todo lo de acá es solo para el administrador: el backend corta por ruta antes
 * de mirar el cuerpo del pedido.
 */

/** CU75: los módulos, las acciones y los permisos especiales que se pueden marcar. */
export async function obtenerCatalogo() {
  const { data } = await axiosClient.get("/permisos/catalogo");
  return data;
}

/** CU75: los cinco roles base y los creados, cada uno con su matriz. */
export async function listarRoles() {
  const { data } = await axiosClient.get("/roles");
  return data;
}

export async function obtenerRol(id) {
  const { data } = await axiosClient.get(`/roles/${id}`);
  return data;
}

/** CU85: crear un rol. El nombre no se puede repetir. */
export async function crearRol({ nombre, descripcion, rolBase, permisos }) {
  const { data } = await axiosClient.post("/roles", { nombre, descripcion, rolBase, permisos });
  return data;
}

/** CU85: cambiar el nombre y la descripción de un rol creado. */
export async function renombrarRol(id, { nombre, descripcion }) {
  const { data } = await axiosClient.patch(`/roles/${id}`, { nombre, descripcion });
  return data;
}

/**
 * CU75: guardar la matriz de un rol.
 *
 * Se manda entera, no casilla por casilla: lo que no viene marcado queda sin
 * permiso. Así dos administradores editando a la vez no terminan con una mezcla
 * de las dos configuraciones.
 */
export async function guardarPermisos(id, permisos) {
  const { data } = await axiosClient.put(`/roles/${id}/permisos`, permisos);
  return data;
}

export async function eliminarRol(id) {
  await axiosClient.delete(`/roles/${id}`);
}

// ------------------------------------------------------------
// Cuentas: trabajan sobre cualquiera de los cinco roles (CU74, CU76)
// ------------------------------------------------------------

export async function obtenerCuenta(id) {
  const { data } = await axiosClient.get(`/cuentas/${id}`);
  return data;
}

/** RF74 y RF76: qué se le hizo a la cuenta, quién y cuándo. */
export async function listarMovimientos(id) {
  const { data } = await axiosClient.get(`/cuentas/${id}/movimientos`);
  return data;
}

/**
 * CU74: asignarle un rol a alguien que ya existe.
 *
 * Si cambia el rol base, el backend muda la cuenta a la tabla del rol nuevo y le
 * cambia el id, así que la respuesta trae el id nuevo y hay que recargar.
 */
export async function asignarRol(id, { rol, rolPersonalizadoId, motivo }) {
  const { data } = await axiosClient.put(`/cuentas/${id}/rol`, {
    rol,
    rolPersonalizadoId: rolPersonalizadoId ?? null,
    motivo: motivo?.trim() || null,
  });
  return data;
}

/** CU76: habilitar o deshabilitar. El motivo es obligatorio al deshabilitar. */
export async function cambiarEstadoDeCuenta(id, { activa, motivo, cancelarReservasFuturas }) {
  const { data } = await axiosClient.put(`/cuentas/${id}/estado`, {
    activa,
    motivo: motivo?.trim() || null,
    cancelarReservasFuturas: !!cancelarReservasFuturas,
  });
  return data;
}
