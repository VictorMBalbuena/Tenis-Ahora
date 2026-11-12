// Modelo del profesor, alineado con ProfesorResponse del backend: Persona + Instructor (certificación, estado de validación) + Profesor (título habilitante).
import { EstadoValidacion, ETIQUETA_VALIDACION } from "@/lib/estadoValidacion";
import { hoyLocal } from "@/lib/fechas";

export { EstadoValidacion, ETIQUETA_VALIDACION };

export function nombreCompleto(profesor) {
  return `${profesor.nombre} ${profesor.apellido}`.trim();
}

export function estaValidado(profesor) {
  return profesor.estadoValidacion === EstadoValidacion.VALIDADO;
}

/** Regla de dominio del módulo: sin la certificación validada no se puede asignar la persona a una clase. */
export function puedeDictarClases(profesor) {
  return Boolean(profesor.activo) && estaValidado(profesor);
}

/** La dirección vive partida en la entidad; para mostrarla se vuelve a unir. */
export function direccionCompleta(profesor) {
  const partes = [profesor.calle, profesor.numeroCalle].filter(Boolean);
  return partes.length ? partes.join(" ") : "—";
}

/** Lo mismo con el teléfono: código de área y número son campos distintos. */
export function telefonoCompleto(profesor) {
  if (!profesor.numeroTelefono) return "—";
  const area = profesor.codigoArea ? `${profesor.codigoArea} ` : "";
  return `${area}${profesor.numeroTelefono}`;
}

/** DNI con puntos, como lo muestran los mockups. */
export function dniFormateado(dni) {
  if (!dni) return "";
  return String(dni).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

/** Fecha ISO (2026-04-18) a formato local (18/04/2026). */
export function fechaCorta(iso) {
  if (!iso) return "—";
  const [anio, mes, dia] = String(iso).split("T")[0].split("-");
  return `${dia}/${mes}/${anio}`;
}

const SOLO_DIGITOS_DNI = /^\d{7,8}$/;
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Validación compartida entre Nuevo/EditarProfesorPage; contraseña y certificación solo son obligatorias en el alta.
export function validarProfesor(datos, { esAlta }) {
  const errores = {};
  if (!datos.nombre.trim()) errores.nombre = "Escribí el nombre.";
  if (!datos.apellido.trim()) errores.apellido = "Escribí el apellido.";
  if (!SOLO_DIGITOS_DNI.test(datos.dni.trim())) errores.dni = "El DNI son 7 u 8 dígitos, sin puntos.";
  if (!CORREO.test(datos.email.trim())) errores.email = "Escribí un correo válido.";
  if (!datos.numeroTelefono.trim()) errores.numeroTelefono = "Escribí el número de teléfono.";
  if (!datos.tituloHabilitante.trim()) errores.tituloHabilitante = "Escribí el título habilitante.";

  if (esAlta) {
    if (!datos.password || datos.password.length < 8) errores.password = "La contraseña tiene que tener al menos 8 caracteres.";
    if (!datos.numeroCertificacion?.trim()) errores.numeroCertificacion = "Escribí el número de certificación.";
    if (!datos.entidadEmisora?.trim()) errores.entidadEmisora = "Escribí la entidad emisora.";
    if (!datos.fechaEmision) errores.fechaEmision = "Elegí la fecha de emisión.";
    // El calendario ya corta en hoy, pero la fecha se puede escribir a mano.
    else if (datos.fechaEmision > hoyLocal()) errores.fechaEmision = "La fecha de emisión no puede ser posterior a hoy.";
  }

  return errores;
}
