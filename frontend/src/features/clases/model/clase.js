import { useAuthStore } from "@/store/authStore";

/** Cupo máximo institucional (RD01). Ninguna clase puede superarlo. */
export const CUPO_MAXIMO_INSTITUCIONAL = 30;

export const NivelClase = {
  INICIAL: "INICIAL",
  INTERMEDIO: "INTERMEDIO",
  AVANZADO: "AVANZADO",
};

export const ETIQUETA_NIVEL = {
  [NivelClase.INICIAL]: "Inicial",
  [NivelClase.INTERMEDIO]: "Intermedio",
  [NivelClase.AVANZADO]: "Avanzado",
};

export const Modalidad = {
  GRUPAL: "GRUPAL",
  PARTICULAR: "PARTICULAR",
};

export const ETIQUETA_MODALIDAD = {
  [Modalidad.GRUPAL]: "Grupal",
  [Modalidad.PARTICULAR]: "Particular",
};

export const EstadoActividad = {
  PROGRAMADA: "PROGRAMADA",
  EN_CURSO: "EN_CURSO",
  FINALIZADA: "FINALIZADA",
  CANCELADA: "CANCELADA",
};

export const ETIQUETA_ESTADO = {
  [EstadoActividad.PROGRAMADA]: "Programada",
  [EstadoActividad.EN_CURSO]: "En curso",
  [EstadoActividad.FINALIZADA]: "Finalizada",
  [EstadoActividad.CANCELADA]: "Cancelada",
};

export const DiaSemana = {
  MONDAY: "MONDAY",
  TUESDAY: "TUESDAY",
  WEDNESDAY: "WEDNESDAY",
  THURSDAY: "THURSDAY",
  FRIDAY: "FRIDAY",
  SATURDAY: "SATURDAY",
  SUNDAY: "SUNDAY",
};

export const ETIQUETA_DIA = {
  [DiaSemana.MONDAY]: "Lunes",
  [DiaSemana.TUESDAY]: "Martes",
  [DiaSemana.WEDNESDAY]: "Miércoles",
  [DiaSemana.THURSDAY]: "Jueves",
  [DiaSemana.FRIDAY]: "Viernes",
  [DiaSemana.SATURDAY]: "Sábado",
  [DiaSemana.SUNDAY]: "Domingo",
};

export const ABREVIATURA_DIA = {
  [DiaSemana.MONDAY]: "Lun",
  [DiaSemana.TUESDAY]: "Mar",
  [DiaSemana.WEDNESDAY]: "Mié",
  [DiaSemana.THURSDAY]: "Jue",
  [DiaSemana.FRIDAY]: "Vie",
  [DiaSemana.SATURDAY]: "Sáb",
  [DiaSemana.SUNDAY]: "Dom",
};

export const ORDEN_DIAS = [
  DiaSemana.MONDAY,
  DiaSemana.TUESDAY,
  DiaSemana.WEDNESDAY,
  DiaSemana.THURSDAY,
  DiaSemana.FRIDAY,
  DiaSemana.SATURDAY,
  DiaSemana.SUNDAY,
];

export const OPCIONES_NIVEL = Object.values(NivelClase).map((v) => ({ value: v, label: ETIQUETA_NIVEL[v] }));
export const OPCIONES_MODALIDAD = Object.values(Modalidad).map((v) => ({ value: v, label: ETIQUETA_MODALIDAD[v] }));
export const OPCIONES_DIA = ORDEN_DIAS.map((v) => ({ value: v, label: ETIQUETA_DIA[v] }));

export function fechaCorta(iso) {
  if (!iso) return "—";
  const [anio, mes, dia] = String(iso).split("T")[0].split("-");
  return `${dia}/${mes}/${anio}`;
}

export function vigencia(clase) {
  return `${fechaCorta(clase.fechaInicio)} a ${fechaCorta(clase.fechaFin)}`;
}

export function diasCortos(clase) {
  const dias = clase.dias ?? [];
  return ORDEN_DIAS.filter((d) => dias.includes(d))
    .map((d) => ETIQUETA_DIA[d].slice(0, 2))
    .join(", ") || "—";
}

export function diasTexto(clase) {
  const dias = clase?.dias ?? [];
  return ORDEN_DIAS.filter((d) => dias.includes(d))
    .map((d) => ABREVIATURA_DIA[d] || ETIQUETA_DIA[d])
    .join(", ") || "—";
}

export function horario(clase) {
  const hhmm = (h) => (h ? String(h).slice(0, 5) : "—");
  return `${hhmm(clase.horaInicio)} a ${hhmm(clase.horaFin)}`;
}

export function lugaresLibres(clase) {
  return Math.max(0, Number(clase.cupoMaximo || 0) - Number(clase.cupoOcupado || 0));
}

export function cupoCompleto(clase) {
  return Number(clase.cupoOcupado || 0) >= Number(clase.cupoMaximo || 0);
}

export function porcentajeOcupado(clase) {
  if (!clase.cupoMaximo) return 0;
  return Math.min(100, Math.round(((Number(clase.cupoOcupado || 0)) / Number(clase.cupoMaximo)) * 100));
}

/** Nombre visible del profesor que la dicta. Si el backend no lo envía, usamos el usuario logueado. */
export function nombreProfesor(clase) {
  // 1. Si la API manda el dato (como pasa en el detalle), lo usamos directo.
  if (clase.profesor) return clase.profesor;

  // 2. Si la API no lo manda (como pasa en la lista), lo armamos con los datos de la sesión.
  try {
    const usuario = useAuthStore.getState().user;
    if (usuario?.rol === "PROFESOR") {
      // Intentamos armar "apellido, nombre" para que coincida con el backend
      if (usuario.nombre && usuario.apellido) {
         return `${usuario.apellido}, ${usuario.nombre}`.toLowerCase();
      }
      if (usuario.nombre) return usuario.nombre.toLowerCase();
      
      // Si no tiene nombre cargado en la base, usamos la primera parte del email (ej: "brunoprofe")
      if (usuario.email) return usuario.email.split("@")[0];
    }
  } catch {
    // Ignorar silenciosamente si falla la lectura del store
  }

  return "—";
}

export function nombreCancha(clase) {
  if (!clase) return "—";
  if (typeof clase.cancha === "string") return clase.cancha;
  if (clase.cancha?.nombre) return clase.cancha.nombre;
  if (clase.cancha?.identificador) return `Cancha ${clase.cancha.identificador}`;
  return "—";
}

export function admiteAsistencia(clase) {
  return clase.estado === EstadoActividad.EN_CURSO || clase.estado === EstadoActividad.FINALIZADA;
}

export function yaPaso(clase) {
  return clase.estado === EstadoActividad.FINALIZADA || clase.estado === EstadoActividad.CANCELADA;
}

export function admiteInscripcion(clase) {
  return clase.estado === EstadoActividad.PROGRAMADA && !cupoCompleto(clase);
}

export function validarClase(datos) {
  const errores = {};

  if (!datos.nombre?.trim()) errores.nombre = "Escribí un nombre para la clase.";
  if (!datos.canchaId) errores.canchaId = "Elegí la cancha.";
  if (!datos.profesorId) errores.profesorId = "Elegí el profesor que la dicta.";
  if (!datos.nivel) errores.nivel = "Elegí el nivel.";
  if (!datos.modalidad) errores.modalidad = "Elegí la modalidad.";

  if (!datos.fechaInicio) errores.fechaInicio = "Elegí la fecha de inicio.";
  if (!datos.fechaFin) errores.fechaFin = "Elegí la fecha de fin.";
  if (datos.fechaInicio && datos.fechaFin && datos.fechaFin <= datos.fechaInicio) {
    errores.fechaFin = "Tiene que ser posterior a la fecha de inicio.";
  }

  if (!datos.dias || datos.dias.length === 0) errores.dias = "Elegí al menos un día de la semana.";

  if (!datos.horaInicio) errores.horaInicio = "Elegí la hora de inicio.";
  if (!datos.horaFin) errores.horaFin = "Elegí la hora de fin.";
  if (datos.horaInicio && datos.horaFin && datos.horaFin <= datos.horaInicio) {
    errores.horaFin = "Tiene que ser posterior a la hora de inicio.";
  }

  if (datos.cupoMaximo !== undefined && datos.cupoMaximo !== "") {
    const cupo = Number(datos.cupoMaximo);
    if (Number.isNaN(cupo)) {
      errores.cupoMaximo = "Escribí el cupo máximo.";
    } else if (cupo < 1) {
      errores.cupoMaximo = "Tiene que ser al menos 1.";
    } else if (cupo > CUPO_MAXIMO_INSTITUCIONAL) {
      errores.cupoMaximo = `El límite del club es ${CUPO_MAXIMO_INSTITUCIONAL} alumnos.`;
    }
  }

  return errores;
}

export function resumirClases(clases, totalElementos = clases.length) {
  return {
    total: totalElementos,
    inscriptos: clases.reduce((acc, c) => acc + Number(c.cupoOcupado || 0), 0),
    completas: clases.filter(cupoCompleto).length,
    conLugar: clases.filter((c) => !cupoCompleto(c)).length,
  };
}