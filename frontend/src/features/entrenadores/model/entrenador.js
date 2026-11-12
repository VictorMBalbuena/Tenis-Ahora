// Modelo del entrenador, alineado con EntrenadorResponse del backend: Persona + Instructor (certificación, estado de validación) + Entrenador (certificación deportiva).
import { EstadoValidacion, ETIQUETA_VALIDACION } from "@/lib/estadoValidacion";
import { hoyLocal } from "@/lib/fechas";

export { EstadoValidacion, ETIQUETA_VALIDACION };

export function nombreCompleto(entrenador) {
  return `${entrenador.nombre} ${entrenador.apellido}`.trim();
}

export function estaValidado(entrenador) {
  return entrenador.estadoValidacion === EstadoValidacion.VALIDADO;
}

/** Regla de dominio del módulo: sin la certificación validada no se puede asignar la persona a un entrenamiento. */
export function puedeDirigirEntrenamientos(entrenador) {
  return Boolean(entrenador.activo) && estaValidado(entrenador);
}

/** La dirección vive partida en la entidad; para mostrarla se vuelve a unir. */
export function direccionCompleta(entrenador) {
  const partes = [entrenador.calle, entrenador.numeroCalle].filter(Boolean);
  return partes.length ? partes.join(" ") : "—";
}

/** Lo mismo con el teléfono: código de área y número son campos distintos. */
export function telefonoCompleto(entrenador) {
  if (!entrenador.numeroTelefono) return "—";
  const area = entrenador.codigoArea ? `${entrenador.codigoArea} ` : "";
  return `${area}${entrenador.numeroTelefono}`;
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

// Validación compartida entre Nuevo/EditarEntrenadorPage; contraseña y certificación solo son obligatorias en el alta.
export function validarEntrenador(datos, { esAlta }) {
  const errores = {};
  if (!datos.nombre?.trim()) errores.nombre = "Escribí el nombre.";
  if (!datos.apellido?.trim()) errores.apellido = "Escribí el apellido.";
  if (!SOLO_DIGITOS_DNI.test(datos.dni?.trim() || "")) errores.dni = "El DNI son 7 u 8 dígitos, sin puntos.";
  if (!CORREO.test(datos.email?.trim() || "")) errores.email = "Escribí un correo válido.";
  if (!datos.numeroTelefono?.trim()) {
    errores.numeroTelefono = "Escribí el número de teléfono.";
  } else if (datos.numeroTelefono.startsWith("15") || datos.numeroTelefono.length < 6 || datos.numeroTelefono.length > 8) {
    errores.numeroTelefono = "Sin el 15, entre 6 y 8 dígitos.";
  }

  if (datos.codigoArea?.trim() && datos.codigoArea.startsWith("0")) {
    errores.codigoArea = "Sin el 0 (ej: 11, 351).";
  }

  if (datos.fechaNacimiento) {
    const hoy = new Date().toISOString().split("T")[0];
    if (datos.fechaNacimiento >= hoy) {
      errores.fechaNacimiento = "La fecha de nacimiento debe ser pasada.";
    }
  }

  if (datos.fechaVencimiento) {
    const hoy = new Date().toISOString().split("T")[0];
    if (datos.fechaVencimiento < hoy) {
      errores.fechaVencimiento = "La certificación está vencida.";
    }
  }

  if (datos.valorPorHora !== "" && datos.valorPorHora !== null && Number(datos.valorPorHora) < 0) {
    errores.valorPorHora = "El valor por hora no puede ser negativo.";
  }

  if (!datos.certificacionDeportiva?.trim()) errores.certificacionDeportiva = "Escribí la certificación deportiva.";

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

/** Mapea respuestas de error del backend (códigos de negocio y campos JSR-380) a los campos del formulario. */
export function mapearErroresBackend(error) {
  const campos = {};
  const codigo = error?.response?.data?.codigo;
  const mensaje = error?.response?.data?.mensaje;
  const backendCampos = error?.response?.data?.campos;

  if (backendCampos && typeof backendCampos === "object") {
    for (const [k, v] of Object.entries(backendCampos)) {
      if (k.includes(".")) {
        const simple = k.split(".").pop();
        if (!campos[simple]) campos[simple] = v;
      } else {
        campos[k] = v;
      }
    }
  }

  if (codigo === "PERSONA_DNI_DUPLICADO") {
    campos.dni = mensaje || "Ya existe una persona con ese DNI.";
  } else if (codigo === "PERSONA_EMAIL_DUPLICADO") {
    campos.email = mensaje || "Ya existe una persona con ese correo.";
  } else if (codigo === "PERSONA_DUPLICADA") {
    campos.dni = mensaje || "Ya existe una persona registrada con este DNI o correo.";
    campos.email = mensaje || "Ya existe una persona registrada con este DNI o correo.";
  } else if (codigo === "CERTIFICACION_DUPLICADA") {
    campos.numeroCertificacion = mensaje || "Ya existe una certificación con ese número.";
  } else if (codigo === "CERTIFICACION_VENCIDA") {
    campos.fechaVencimiento = mensaje || "La certificación está vencida.";
  } else if (codigo === "ENTRENADOR_SIN_CERTIFICACION") {
    campos.certificacionDeportiva = mensaje || "Falta la certificación deportiva.";
  }

  return campos;
}
