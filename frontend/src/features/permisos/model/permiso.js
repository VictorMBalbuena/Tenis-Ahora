/**
 * Modelo de la feature de roles y permisos (CU74, CU75, CU76, CU85).
 *
 * Mapeado contra RolResponse, MatrizDePermisos, CuentaResponse y
 * MovimientoResponse del backend.
 *
 * Las etiquetas de los módulos, las acciones y los permisos especiales NO están
 * escritas acá: vienen del catálogo que da el backend (GET /permisos/catalogo),
 * así no hay dos listas que se desincronicen.
 */

export const RolBase = {
  SOCIO: "SOCIO",
  EMPLEADO: "EMPLEADO",
  ADMINISTRADOR: "ADMINISTRADOR",
  PROFESOR: "PROFESOR",
  ENTRENADOR: "ENTRENADOR",
};

export const ETIQUETA_ROL_BASE = {
  SOCIO: "Socio",
  EMPLEADO: "Empleado",
  ADMINISTRADOR: "Administrador",
  PROFESOR: "Profesor",
  ENTRENADOR: "Entrenador",
};

export const ETIQUETA_MOVIMIENTO = {
  CAMBIO_DE_ROL: "Cambio de rol",
  DESHABILITACION: "Baja",
  HABILITACION: "Alta",
};

/** Opciones para un desplegable de rol base. */
export const OPCIONES_ROL_BASE = Object.keys(RolBase).map((valor) => ({
  value: valor,
  label: ETIQUETA_ROL_BASE[valor],
}));

export function etiquetaDeRolBase(rol) {
  return ETIQUETA_ROL_BASE[rol] ?? rol ?? "—";
}

/** Si el rol tiene marcada esa acción en ese módulo. */
export function tienePermiso(permisos, modulo, accion) {
  return !!permisos?.modulos?.[modulo]?.includes(accion);
}

export function tieneEspecial(permisos, especial) {
  return !!permisos?.especiales?.includes(especial);
}

/**
 * Devuelve una matriz nueva con esa casilla invertida. No muta la que recibe:
 * el estado de React tiene que cambiar de identidad para que la grilla se
 * vuelva a dibujar.
 */
export function alternarPermiso(permisos, modulo, accion) {
  const actuales = permisos?.modulos?.[modulo] ?? [];
  const siguientes = actuales.includes(accion)
    ? actuales.filter((a) => a !== accion)
    : [...actuales, accion];

  const modulos = { ...(permisos?.modulos ?? {}) };
  if (siguientes.length === 0) {
    delete modulos[modulo];
  } else {
    modulos[modulo] = siguientes;
  }
  return { modulos, especiales: permisos?.especiales ?? [] };
}

export function alternarEspecial(permisos, especial) {
  const actuales = permisos?.especiales ?? [];
  const siguientes = actuales.includes(especial)
    ? actuales.filter((e) => e !== especial)
    : [...actuales, especial];
  return { modulos: permisos?.modulos ?? {}, especiales: siguientes };
}

/** Marca o desmarca todas las acciones de un módulo de una sola vez. */
export function alternarModuloEntero(permisos, modulo, acciones) {
  const actuales = permisos?.modulos?.[modulo] ?? [];
  const completo = actuales.length === acciones.length;

  const modulos = { ...(permisos?.modulos ?? {}) };
  if (completo) {
    delete modulos[modulo];
  } else {
    modulos[modulo] = [...acciones];
  }
  return { modulos, especiales: permisos?.especiales ?? [] };
}

/** Cuántas casillas tiene marcadas un rol, para mostrarlo en el listado. */
export function contarPermisos(permisos) {
  const deModulos = Object.values(permisos?.modulos ?? {}).reduce((total, acciones) => total + acciones.length, 0);
  return deModulos + (permisos?.especiales?.length ?? 0);
}

/** Dos matrices son iguales si marcan exactamente lo mismo, sin importar el orden. */
export function mismosPermisos(a, b) {
  return JSON.stringify(normalizar(a)) === JSON.stringify(normalizar(b));
}

function normalizar(permisos) {
  const modulos = {};
  Object.keys(permisos?.modulos ?? {})
    .sort()
    .forEach((modulo) => {
      const acciones = [...(permisos.modulos[modulo] ?? [])].sort();
      if (acciones.length > 0) modulos[modulo] = acciones;
    });
  return { modulos, especiales: [...(permisos?.especiales ?? [])].sort() };
}

/**
 * Valida el alta de un rol (CU85). El nombre repetido lo detecta el backend,
 * que es el único que puede saberlo sin condiciones de carrera.
 */
export function validarRol(datos) {
  const errores = {};

  if (!datos.nombre?.trim()) {
    errores.nombre = "El nombre del rol es obligatorio.";
  } else if (datos.nombre.trim().length < 2) {
    errores.nombre = "El nombre tiene que tener al menos 2 caracteres.";
  } else if (datos.nombre.trim().length > 60) {
    errores.nombre = "El nombre no puede superar los 60 caracteres.";
  }

  if (datos.descripcion && datos.descripcion.trim().length > 200) {
    errores.descripcion = "La descripción no puede superar los 200 caracteres.";
  }

  if (!datos.rolBase) {
    errores.rolBase = "Elegí sobre qué rol base se apoya.";
  }

  return { valido: Object.keys(errores).length === 0, errores };
}

/** Valida la baja de una cuenta (CU76): RF76 exige el motivo. */
export function validarCambioDeEstado({ activa, motivo }) {
  const errores = {};
  if (!activa && !motivo?.trim()) {
    errores.motivo = "Para deshabilitar una cuenta hay que indicar el motivo.";
  } else if (motivo && motivo.trim().length > 300) {
    errores.motivo = "El motivo no puede superar los 300 caracteres.";
  }
  return { valido: Object.keys(errores).length === 0, errores };
}

/** El rol Administrador base tiene siempre todos los permisos: no se muestra para editar. */
export function rolesEditables(roles = []) {
  return roles.filter((rol) => !(rol.base && rol.rolBase === "ADMINISTRADOR"));
}
