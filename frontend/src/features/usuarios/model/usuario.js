import { hoyLocal } from "@/lib/fechas";
/**
 * Modelo y validaciones de dominio para el módulo de usuarios.
 *
 * Mapeado contra PersonaRequest, PersonaUpdateRequest y PersonaResumenResponse
 * del backend en Spring Boot.
 */

export const TipoUsuario = {
  SOCIO: "SOCIO",
  EMPLEADO: "EMPLEADO",
  ADMINISTRADOR: "ADMINISTRADOR",
};

export const ETIQUETA_TIPO = {
  [TipoUsuario.SOCIO]: "Socios",
  [TipoUsuario.EMPLEADO]: "Empleados",
  [TipoUsuario.ADMINISTRADOR]: "Administradores",
};

// Expresiones regulares de validación acordes a las anotaciones del backend
export const REGEX_NOMBRE_PERSONA = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s'-]+$/;
export const REGEX_DNI = /^\d{7,8}$/;
export const REGEX_CODIGO_AREA = /^[1-9]\d{1,3}$/;
export const REGEX_TELEFONO = /^\d{6,8}$/;
export const REGEX_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const REGEX_NUMERO_CALLE = /^\d+$/;

/**
 * Valida los campos de alta o modificación de una persona.
 *
 * @param {Object} datos
 * @param {boolean} esEdicion
 * @returns {{ valido: boolean, errores: Record<string, string> }}
 */
export function validarPersona(datos, esEdicion = false) {
  const errores = {};

  if (!datos.nombre?.trim()) {
    errores.nombre = "El nombre es obligatorio.";
  } else if (datos.nombre.trim().length > 100) {
    errores.nombre = "El nombre no puede superar los 100 caracteres.";
  } else if (!REGEX_NOMBRE_PERSONA.test(datos.nombre.trim())) {
    errores.nombre = "El nombre solo puede contener letras y espacios.";
  }

  if (!datos.apellido?.trim()) {
    errores.apellido = "El apellido es obligatorio.";
  } else if (datos.apellido.trim().length > 100) {
    errores.apellido = "El apellido no puede superar los 100 caracteres.";
  } else if (!REGEX_NOMBRE_PERSONA.test(datos.apellido.trim())) {
    errores.apellido = "El apellido solo puede contener letras y espacios.";
  }

  if (!datos.dni?.trim()) {
    errores.dni = "El DNI es obligatorio.";
  } else if (!REGEX_DNI.test(datos.dni.trim())) {
    errores.dni = "El DNI debe tener 7 u 8 dígitos numéricos.";
  }

  if (!esEdicion) {
    if (!datos.fechaNacimiento) {
      errores.fechaNacimiento = "La fecha de nacimiento es obligatoria.";
    } else {
      const hoy = hoyLocal();
      if (datos.fechaNacimiento >= hoy) {
        errores.fechaNacimiento = "La fecha de nacimiento debe ser pasada.";
      }
    }
  } else if (datos.fechaNacimiento) {
    const hoy = hoyLocal();
    if (datos.fechaNacimiento >= hoy) {
      errores.fechaNacimiento = "La fecha de nacimiento debe ser pasada.";
    }
  }

  if (!datos.calle?.trim()) {
    errores.calle = "La calle es obligatoria.";
  } else if (datos.calle.trim().length > 150) {
    errores.calle = "La calle no puede superar los 150 caracteres.";
  }

  if (!datos.numeroCalle?.trim()) {
    errores.numeroCalle = "El número de calle es obligatorio.";
  } else if (!REGEX_NUMERO_CALLE.test(datos.numeroCalle.trim())) {
    errores.numeroCalle = "Solo se permiten números.";
  }

  if (!datos.codigoArea?.trim()) {
    errores.codigoArea = "El código de área es obligatorio.";
  } else if (!REGEX_CODIGO_AREA.test(datos.codigoArea.trim())) {
    errores.codigoArea = "Sin el 0 (ej: 11, 351).";
  }

  if (!datos.numeroTelefono?.trim()) {
    errores.numeroTelefono = "El número de teléfono es obligatorio.";
  } else if (!REGEX_TELEFONO.test(datos.numeroTelefono.trim())) {
    errores.numeroTelefono = "Sin el 15, entre 6 y 8 dígitos.";
  }

  if (!datos.email?.trim()) {
    errores.email = "El correo electrónico es obligatorio.";
  } else if (!REGEX_EMAIL.test(datos.email.trim())) {
    errores.email = "El correo electrónico no tiene un formato válido.";
  } else if (datos.email.trim().length > 255) {
    errores.email = "El correo no puede superar los 255 caracteres.";
  }

  // La contraseña solo se exige y valida en el alta (en edición no se modifica por acá)
  if (!esEdicion) {
    if (!datos.password?.trim()) {
      errores.password = "La contraseña es obligatoria.";
    } else if (datos.password.length < 8 || datos.password.length > 72) {
      errores.password = "La contraseña debe tener entre 8 y 72 caracteres.";
    }
  }

  return {
    valido: Object.keys(errores).length === 0,
    errores,
  };
}

/** Nombre completo para visualización */
export function nombreCompleto(usuario) {
  if (!usuario) return "";
  return `${usuario.nombre || ""} ${usuario.apellido || ""}`.trim();
}

/** Formateador de DNI con puntos: 30123456 -> 30.123.456 */
export function dniFormateado(dni) {
  if (!dni) return "—";
  return String(dni).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

/** Fecha ISO YYYY-MM-DD a formato local DD/MM/YYYY */
export function fechaCorta(iso) {
  if (!iso) return "—";
  const partes = String(iso).split("-");
  if (partes.length !== 3) return iso;
  const [anio, mes, dia] = partes;
  return `${dia}/${mes}/${anio}`;
}

/** Teléfono formateado con código de área: (11) 12345678 */
export function telefonoFormateado(codigoArea, numero) {
  if (!codigoArea && !numero) return "—";
  return `(${codigoArea || ""}) ${numero || ""}`.trim();
}

/** Domicilio formateado: Calle 123 */
export function domicilioFormateado(calle, numeroCalle) {
  if (!calle && !numeroCalle) return "—";
  return `${calle || ""} ${numeroCalle || ""}`.trim();
}

/** Resumen métrico para las tarjetas del encabezado */
export function resumirUsuarios(contenido = []) {
  return {
    total: contenido.length,
    activos: contenido.filter((u) => u.activo).length,
    inactivos: contenido.filter((u) => !u.activo).length,
  };
}
