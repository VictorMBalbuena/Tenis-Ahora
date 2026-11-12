// Modelo del perfil propio: los datos de Persona que la persona puede ver y editar de sí misma.
import { validarPersona } from "@/features/usuarios/model/usuario";

export { fechaCorta, nombreCompleto, dniFormateado, telefonoFormateado, domicilioFormateado } from "@/features/usuarios/model/usuario";

export const ETIQUETA_ROL = {
  ADMINISTRADOR: "Administrador",
  EMPLEADO: "Empleado",
  PROFESOR: "Profesor",
  ENTRENADOR: "Entrenador",
  SOCIO: "Socio",
};

/**
 * Lo que cada quien puede cambiar de sus propios datos.
 *
 * El DNI no está: es la identidad de la persona en el club y la usan el
 * carnet y la facturación, así que si está mal lo corrige un empleado. El rol
 * tampoco, por razones obvias.
 */
export const CAMPOS_EDITABLES = [
  "nombre",
  "apellido",
  "fechaNacimiento",
  "calle",
  "numeroCalle",
  "codigoArea",
  "numeroTelefono",
  "email",
  "sexo",
  "localidad",
];

export const OPCIONES_SEXO = [
  { value: "MASCULINO", label: "Masculino" },
  { value: "FEMENINO", label: "Femenino" },
];

export const MOTIVO_DNI_FIJO =
  "El DNI identifica a la persona en el club y figura en el carnet, así que lo corrige un empleado desde Usuarios.";

/** Las dos iniciales para el avatar cuando no hay foto. */
export function iniciales(perfil) {
  const n = perfil?.nombre?.trim()?.[0] ?? "";
  const a = perfil?.apellido?.trim()?.[0] ?? "";
  const iniciales = `${n}${a}`.toUpperCase();
  if (iniciales) return iniciales;
  return perfil?.email?.trim()?.[0]?.toUpperCase() ?? "?";
}

/** Los datos del perfil, con los del formulario en el formato que espera el input. */
export function aDatosDeFormulario(perfil) {
  return {
    nombre: perfil.nombre ?? "",
    apellido: perfil.apellido ?? "",
    dni: perfil.dni ?? "",
    fechaNacimiento: perfil.fechaNacimiento ?? "",
    calle: perfil.calle ?? "",
    numeroCalle: perfil.numeroCalle ?? "",
    codigoArea: perfil.codigoArea ?? "",
    numeroTelefono: perfil.numeroTelefono ?? "",
    email: perfil.email ?? "",
    sexo: perfil.sexo ?? "",
    localidad: perfil.localidad ?? "",
  };
}

/** Solo lo que cambió, para que el PATCH no pise campos que nadie tocó. */
export function soloLoQueCambio(datos, original) {
  const cambios = {};
  for (const campo of CAMPOS_EDITABLES) {
    if (datos[campo] !== original[campo]) cambios[campo] = datos[campo];
  }
  return cambios;
}

/**
 * Validación de los datos propios.
 *
 * Reusa validarPersona del módulo de usuarios para no tener dos reglas
 * distintas para el mismo dato: el DNI viaja aunque no se edite, porque esa
 * función lo exige.
 */
export function validarPerfil(datos) {
  return validarPersona(datos, true).errores;
}

export const LARGO_MINIMO_PASSWORD = 8;
export const LARGO_MAXIMO_PASSWORD = 72;

/**
 * Validación del cambio de contraseña.
 *
 * El largo es el mismo que pide el registro (8 a 72). Se agrega que la nueva
 * no puede ser igual a la actual, que es el error que más se comete.
 */
export function validarCambioPassword({ passwordActual, passwordNueva, repetir }) {
  const errores = {};

  if (!passwordActual) {
    errores.passwordActual = "Escribí tu contraseña actual.";
  }

  if (!passwordNueva) {
    errores.passwordNueva = "Escribí la contraseña nueva.";
  } else if (passwordNueva.length < LARGO_MINIMO_PASSWORD || passwordNueva.length > LARGO_MAXIMO_PASSWORD) {
    errores.passwordNueva = `Tiene que tener entre ${LARGO_MINIMO_PASSWORD} y ${LARGO_MAXIMO_PASSWORD} caracteres.`;
  } else if (passwordActual && passwordNueva === passwordActual) {
    errores.passwordNueva = "Tiene que ser distinta de la actual.";
  }

  if (passwordNueva && repetir !== passwordNueva) {
    errores.repetir = "Las dos contraseñas no coinciden.";
  }

  return errores;
}

/**
 * Qué tan fuerte es una contraseña, para la barra de la pantalla.
 *
 * No bloquea nada: el backend solo exige el largo. Es una ayuda para que no
 * elijan "12345678", que cumple el largo y no protege nada.
 */
export function fortalezaPassword(password = "") {
  if (!password) return { nivel: 0, etiqueta: "" };

  let puntos = 0;
  if (password.length >= LARGO_MINIMO_PASSWORD) puntos++;
  if (password.length >= 12) puntos++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) puntos++;
  if (/\d/.test(password)) puntos++;
  if (/[^A-Za-z0-9]/.test(password)) puntos++;

  const nivel = Math.min(3, Math.max(1, puntos - 1));
  return { nivel, etiqueta: ["", "Débil", "Aceptable", "Fuerte"][nivel] };
}

export const TIPOS_IMAGEN = ["image/jpeg", "image/png", "image/webp"];
export const TAMANIO_MAXIMO_FOTO = 2 * 1024 * 1024;

/**
 * Revisa la foto antes de subirla.
 *
 * Se corta acá para no hacerle esperar una subida de 10 MB a alguien con
 * datos móviles y que el servidor la rechace al final.
 */
export function validarFoto(archivo) {
  if (!archivo) return "Elegí una imagen.";
  if (!TIPOS_IMAGEN.includes(archivo.type)) return "La foto tiene que ser JPG, PNG o WebP.";
  if (archivo.size > TAMANIO_MAXIMO_FOTO) return "La foto no puede pesar más de 2 MB.";
  return "";
}

export const PREFERENCIAS_POR_DEFECTO = {
  recordatorioReservas: true,
  recordatorioClases: true,
  novedadesDelClub: false,
};

export const ETIQUETA_PREFERENCIA = {
  recordatorioReservas: "Recordarme mis reservas de cancha",
  recordatorioClases: "Recordarme mis clases y entrenamientos",
  novedadesDelClub: "Novedades y torneos del club",
};

export const DETALLE_PREFERENCIA = {
  recordatorioReservas: "Un correo el día anterior, con la cancha y el horario.",
  recordatorioClases: "Un correo el día anterior de cada clase o entrenamiento.",
  novedadesDelClub: "Avisos de torneos nuevos y novedades. No incluye los recordatorios.",
};

/** Completa con los valores por defecto lo que el backend no haya mandado. */
export function conPreferenciasPorDefecto(preferencias) {
  return { ...PREFERENCIAS_POR_DEFECTO, ...(preferencias ?? {}) };
}
