// Modelo del entrenamiento, alineado con EntrenamientoResponse real: una actividad recurrente (rango de fechas + días de la semana), no una fecha puntual.

/** Cupo máximo institucional (RD01, RD04). Vale para toda actividad grupal. */
export const CUPO_MAXIMO_INSTITUCIONAL = 30;

export const Modalidad = {
  GRUPAL: "GRUPAL",
  PARTICULAR: "PARTICULAR",
};

export const ETIQUETA_MODALIDAD = {
  [Modalidad.GRUPAL]: "Grupal",
  [Modalidad.PARTICULAR]: "Particular",
};

export const OPCIONES_MODALIDAD = Object.values(Modalidad).map((v) => ({ value: v, label: ETIQUETA_MODALIDAD[v] }));

export const EstadoActividad = {
  PROGRAMADA: "PROGRAMADA",
  EN_CURSO: "EN_CURSO",
  FINALIZADA: "FINALIZADA",
  CANCELADA: "CANCELADA",
};

export const ETIQUETA_ESTADO = {
  [EstadoActividad.PROGRAMADA]: "Programado",
  [EstadoActividad.EN_CURSO]: "En curso",
  [EstadoActividad.FINALIZADA]: "Finalizado",
  [EstadoActividad.CANCELADA]: "Cancelado",
};

export const OPCIONES_ESTADO = Object.values(EstadoActividad).map((v) => ({ value: v, label: ETIQUETA_ESTADO[v] }));

/** Días de la semana en el orden en que se muestran, con el nombre que serializa java.time.DayOfWeek. */
export const DIAS_SEMANA = [
  { value: "MONDAY", abreviatura: "Lun", etiqueta: "Lunes" },
  { value: "TUESDAY", abreviatura: "Mar", etiqueta: "Martes" },
  { value: "WEDNESDAY", abreviatura: "Mié", etiqueta: "Miércoles" },
  { value: "THURSDAY", abreviatura: "Jue", etiqueta: "Jueves" },
  { value: "FRIDAY", abreviatura: "Vie", etiqueta: "Viernes" },
  { value: "SATURDAY", abreviatura: "Sáb", etiqueta: "Sábado" },
  { value: "SUNDAY", abreviatura: "Dom", etiqueta: "Domingo" },
];

/** Fecha ISO (2026-04-18) a formato local (18/04/2026). */
export function fechaCorta(iso) {
  if (!iso) return "—";
  const [anio, mes, dia] = String(iso).split("T")[0].split("-");
  return `${dia}/${mes}/${anio}`;
}

/** Vigencia del entrenamiento: desde que empieza hasta que termina. */
export function rangoFechas(entrenamiento) {
  return `${fechaCorta(entrenamiento.fechaInicio)} al ${fechaCorta(entrenamiento.fechaFin)}`;
}

/** Días en que se dicta, abreviados y en orden de semana: "Lun, Mié, Vie". */
export function diasTexto(entrenamiento) {
  const dias = entrenamiento.dias || [];
  const texto = DIAS_SEMANA.filter((d) => dias.includes(d.value))
    .map((d) => d.abreviatura)
    .join(", ");
  return texto || "—";
}

/** Horario semanal: "07:00 a 09:00". Las horas llegan como HH:mm o HH:mm:ss. */
export function horario(entrenamiento) {
  const hhmm = (h) => (h ? String(h).slice(0, 5) : "—");
  return `${hhmm(entrenamiento.horaInicio)} a ${hhmm(entrenamiento.horaFin)}`;
}

/** Cuántos lugares quedan. Si el backend no envía cupoDisponible, se calcula desde cupoMaximo - cupoOcupado. */
export function lugaresLibres(entrenamiento) {
  if (!entrenamiento) return 0;
  if (entrenamiento.cupoDisponible != null) {
    return Math.max(0, entrenamiento.cupoDisponible);
  }
  const max = entrenamiento.cupoMaximo ?? 0;
  const ocupado = entrenamiento.cupoOcupado ?? entrenamiento.inscriptos ?? 0;
  return Math.max(0, max - ocupado);
}

/** Con el cupo completo no se puede inscribir (RD01, RD04). */
export function cupoCompleto(entrenamiento) {
  return lugaresLibres(entrenamiento) === 0;
}

export function porcentajeOcupado(entrenamiento) {
  if (!entrenamiento || !entrenamiento.cupoMaximo) return 0;
  const ocupado = entrenamiento.cupoOcupado ?? entrenamiento.inscriptos ?? 0;
  return Math.min(100, Math.round((ocupado / entrenamiento.cupoMaximo) * 100));
}

/** Nombre visible del entrenador: soporta string formateado ("Apellido, Nombre") u objeto ({ nombre, apellido }). */
export function nombreEntrenador(entrenamiento) {
  if (!entrenamiento?.entrenador) return "—";
  if (typeof entrenamiento.entrenador === "object") {
    const { nombre = "", apellido = "" } = entrenamiento.entrenador;
    return `${nombre} ${apellido}`.trim() || "—";
  }
  return String(entrenamiento.entrenador) || "—";
}

/** Identificación visible de la cancha: soporta string formateado ("Cancha 1") u objeto ({ identificador, nombre }). */
export function nombreCancha(entrenamiento) {
  if (!entrenamiento?.cancha) return "—";
  if (typeof entrenamiento.cancha === "object") {
    return entrenamiento.cancha.identificador || entrenamiento.cancha.nombre || "—";
  }
  return String(entrenamiento.cancha) || "—";
}

/** Un entrenamiento finalizado o cancelado ya no admite inscripciones nuevas (CU82). */
export function yaPaso(entrenamiento) {
  return entrenamiento.estado === EstadoActividad.FINALIZADA || entrenamiento.estado === EstadoActividad.CANCELADA;
}

/** Admite inscripciones si está programado o en curso y todavía no completó el cupo (CU82). */
export function admiteInscripcion(entrenamiento) {
  return (
    Boolean(entrenamiento) &&
    (entrenamiento.estado === EstadoActividad.PROGRAMADA || entrenamiento.estado === EstadoActividad.EN_CURSO) &&
    !cupoCompleto(entrenamiento)
  );
}

/** La asistencia se registra en todo entrenamiento activo (no cancelado) dentro de su ciclo (CU43). */
export function admiteAsistencia(entrenamiento) {
  return Boolean(entrenamiento) && Boolean(entrenamiento.estado) && entrenamiento.estado !== EstadoActividad.CANCELADA;
}

/**
 * Validación del formulario de alta y modificación (CU37, CU38).
 * Devuelve { campo: mensaje }; vacío si está todo bien.
 */
export function validarEntrenamiento(datos) {
  const errores = {};

  if (!datos.nombre?.trim()) errores.nombre = "Escribí el nombre del entrenamiento.";
  if (!datos.canchaId) errores.canchaId = "Elegí la cancha.";
  if (!datos.entrenadorId) errores.entrenadorId = "Elegí el entrenador que lo dirige.";
  if (!datos.modalidad) errores.modalidad = "Elegí la modalidad.";
  if (!datos.objetivo?.trim()) errores.objetivo = "Escribí el objetivo del entrenamiento.";

  if (!datos.fechaInicio) errores.fechaInicio = "Elegí la fecha de inicio.";
  if (!datos.fechaFin) errores.fechaFin = "Elegí la fecha de fin.";
  if (datos.fechaInicio && datos.fechaFin && datos.fechaFin < datos.fechaInicio) {
    errores.fechaFin = "Tiene que ser posterior a la fecha de inicio.";
  }

  if (!datos.dias || datos.dias.length === 0) errores.dias = "Elegí al menos un día de la semana.";

  if (!datos.horaInicio) errores.horaInicio = "Elegí la hora de inicio.";
  if (!datos.horaFin) errores.horaFin = "Elegí la hora de fin.";
  if (datos.horaInicio && datos.horaFin && datos.horaFin <= datos.horaInicio) {
    errores.horaFin = "Tiene que ser posterior a la hora de inicio.";
  }

  const cupo = Number(datos.cupoMaximo);
  if (datos.cupoMaximo === "" || Number.isNaN(cupo)) {
    errores.cupoMaximo = "Escribí el cupo máximo.";
  } else if (cupo < 1) {
    errores.cupoMaximo = "Tiene que ser al menos 1.";
  } else if (cupo > CUPO_MAXIMO_INSTITUCIONAL) {
    errores.cupoMaximo = `El límite del club es ${CUPO_MAXIMO_INSTITUCIONAL} alumnos.`;
  }

  return errores;
}

/** Resumen de las tarjetas del listado. `totalElementos` viene del backend. */
export function resumirEntrenamientos(entrenamientos, totalElementos = entrenamientos.length) {
  return {
    total: totalElementos,
    inscriptos: entrenamientos.reduce((acc, e) => acc + (e.cupoOcupado ?? e.inscriptos ?? 0), 0),
    completos: entrenamientos.filter(cupoCompleto).length,
    conLugar: entrenamientos.filter((e) => !cupoCompleto(e)).length,
  };
}
