import axiosClient from "@/config/axiosClient";

/**
 * Perfil y configuración de la persona que está usando el sistema.
 *
 * Todo cuelga de /me y nunca lleva un id: el backend sabe quién es por el
 * token. Es a propósito, porque /socios y /empleados exigen rol EMPLEADO o
 * ADMINISTRADOR, así que por esas rutas un socio no podría editar sus
 * propios datos.
 *
 * Nada de esto existe todavía en el backend: son las rutas que las pantallas
 * esperan. El contrato está escrito en el README de esta carpeta.
 */

const RECURSO = "/me";

/** Perfil completo: los datos de Persona más el rol y, si es socio, su número. */
export async function obtenerMiPerfil() {
  const { data } = await axiosClient.get(RECURSO);
  return data;
}

/** Modificación parcial de los datos propios. Solo viajan los campos que cambiaron. */
export async function actualizarMiPerfil(datos) {
  const { data } = await axiosClient.patch(RECURSO, datos);
  return data;
}

/**
 * Cambia la foto de perfil.
 *
 * Va como multipart porque sube un archivo. El backend responde con la URL
 * ya guardada, así que la pantalla no tiene que adivinar dónde quedó.
 */
export async function subirMiFoto(archivo) {
  const cuerpo = new FormData();
  cuerpo.append("archivo", archivo);

  const { data } = await axiosClient.post(`${RECURSO}/foto`, cuerpo, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

/** Vuelve a la inicial del nombre en vez de una foto. */
export async function quitarMiFoto() {
  await axiosClient.delete(`${RECURSO}/foto`);
}

/**
 * Cambia la contraseña.
 *
 * Pide la actual además de la nueva: si alguien se dejó la sesión abierta,
 * que no pueda cambiarle la contraseña a otra persona.
 */
export async function cambiarMiPassword({ passwordActual, passwordNueva }) {
  await axiosClient.put(`${RECURSO}/password`, { passwordActual, passwordNueva });
}

/** Preferencias de notificaciones. */
export async function obtenerMisPreferencias() {
  const { data } = await axiosClient.get(`${RECURSO}/preferencias`);
  return data;
}

export async function guardarMisPreferencias(preferencias) {
  const { data } = await axiosClient.put(`${RECURSO}/preferencias`, preferencias);
  return data;
}

/**
 * Cierra la sesión en todos los dispositivos.
 *
 * Revoca los refresh tokens de la persona, no solo el de esta pestaña: es lo
 * que hay que hacer si se perdió el teléfono.
 */
export async function cerrarTodasMisSesiones() {
  await axiosClient.post("/auth/logout-all", {});
}
