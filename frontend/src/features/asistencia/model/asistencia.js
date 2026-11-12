import { hoyLocal } from "@/lib/fechas";

/** Modelo de la planilla de asistencia, compartido por clases (CU42) y entrenamientos (CU43). */

export const DIAS_SEMANA_INGLES = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];

export const NOMBRES_DIAS_ES = {
  MONDAY: "lunes",
  TUESDAY: "martes",
  WEDNESDAY: "miércoles",
  THURSDAY: "jueves",
  FRIDAY: "viernes",
  SATURDAY: "sábado",
  SUNDAY: "domingo",
};

/** Devuelve el día de la semana en formato enum Java (MONDAY..SUNDAY) para una fecha ISO AAAA-MM-DD. */
export function diaDeSemanaIso(fechaIso) {
  if (!fechaIso) return null;
  const [anio, mes, dia] = fechaIso.split("T")[0].split("-").map(Number);
  if (!anio || !mes || !dia) return null;
  const date = new Date(anio, mes - 1, dia);
  return DIAS_SEMANA_INGLES[date.getDay()];
}

/** Fecha ISO (AAAA-MM-DD) a formato legible local (DD/MM/AAAA). */
export function formatearFechaLocal(iso) {
  if (!iso) return "—";
  const [anio, mes, dia] = String(iso).split("T")[0].split("-");
  return `${dia}/${mes}/${anio}`;
}

/** Resta N días a una fecha ISO AAAA-MM-DD y devuelve el nuevo ISO. */
export function restarDias(fechaIso, dias = 1) {
  if (!fechaIso) return "";
  const [a, m, d] = fechaIso.split("T")[0].split("-").map(Number);
  const dt = new Date(a, m - 1, d);
  dt.setDate(dt.getDate() - dias);
  const pad = (n) => String(n).padStart(2, "0");
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`;
}

/** Considera nocturna una sesión cuya finalización sea a las 19:00 o posterior. */
export function esHorarioNocturno(horaFin) {
  if (!horaFin) return false;
  return String(horaFin).slice(0, 5) >= "19:00";
}

/** Devuelve la última fecha pasada en que se dictó la actividad (hasta 14 días atrás). */
export function ultimaFechaDictado(actividad, desdeFechaIso = hoyLocal()) {
  if (!actividad) return null;
  const dias = Array.isArray(actividad.dias)
    ? actividad.dias
    : actividad.dias instanceof Set
    ? Array.from(actividad.dias)
    : [];
  if (dias.length === 0) return null;

  const inicio = actividad.fechaInicio ? actividad.fechaInicio.split("T")[0] : "";
  let actual = restarDias(desdeFechaIso, 1);
  for (let i = 0; i < 14; i++) {
    if (inicio && actual < inicio) break;
    const dia = diaDeSemanaIso(actual);
    if (dias.includes(dia)) {
      return actual;
    }
    actual = restarDias(actual, 1);
  }
  return null;
}

/**
 * Sugiere la fecha más adecuada para tomar asistencia.
 * Si hoy es día de dictado, sugiere hoy.
 * Si la actividad tuvo clase anoche (horario nocturno) y aún es la mañana siguiente (<14hs),
 * sugiere la fecha de anoche con ventana de gracia.
 */
export function fechaSugeridaAsistencia(actividad, hoyIso = hoyLocal(), hora = new Date().getHours()) {
  if (!actividad) return { fecha: hoyIso, esPeriodoGracia: false };

  const dias = Array.isArray(actividad.dias)
    ? actividad.dias
    : actividad.dias instanceof Set
    ? Array.from(actividad.dias)
    : [];

  const inicio = actividad.fechaInicio ? actividad.fechaInicio.split("T")[0] : "";
  const fin = actividad.fechaFin ? actividad.fechaFin.split("T")[0] : "";

  // 1. Si hoy está dentro de la vigencia y es día de dictado -> Hoy
  const diaHoy = diaDeSemanaIso(hoyIso);
  const hoyEnVigencia = (!inicio || hoyIso >= inicio) && (!fin || hoyIso <= fin);
  if (hoyEnVigencia && dias.includes(diaHoy)) {
    return { fecha: hoyIso, esPeriodoGracia: false };
  }

  // 2. Si anoche hubo clase en horario nocturno y estamos en la mañana siguiente (< 14 hs)
  const ayerIso = restarDias(hoyIso, 1);
  const diaAyer = diaDeSemanaIso(ayerIso);
  const ayerEnVigencia = (!inicio || ayerIso >= inicio) && (!fin || ayerIso <= fin);
  if (ayerEnVigencia && dias.includes(diaAyer) && esHorarioNocturno(actividad.horaFin) && hora < 14) {
    return {
      fecha: ayerIso,
      esPeriodoGracia: true,
      motivo: `Clase nocturna de anoche (${formatearFechaLocal(ayerIso)}) en período de gracia.`,
    };
  }

  return { fecha: hoyIso, esPeriodoGracia: false };
}

/**
 * Valida si en una fecha puntual se puede tomar asistencia para una actividad.
 * Devuelve { valida: boolean, motivo?: string }
 */
export function validarFechaAsistencia(actividad, fechaIso, hoyIso = hoyLocal()) {
  if (!actividad || !fechaIso) return { valida: true };

  if (fechaIso > hoyIso) {
    return {
      valida: false,
      motivo: "No se puede registrar asistencia de una fecha futura.",
    };
  }

  const inicio = actividad.fechaInicio ? actividad.fechaInicio.split("T")[0] : "";
  if (inicio && fechaIso < inicio) {
    if (fechaIso === hoyIso) {
      return {
        valida: false,
        motivo: `Esta actividad todavía no comenzó (fecha de inicio: ${formatearFechaLocal(inicio)}). No es posible tomar asistencia antes del inicio.`,
      };
    }
    return {
      valida: false,
      motivo: `La fecha seleccionada (${formatearFechaLocal(fechaIso)}) es anterior al inicio de la actividad (${formatearFechaLocal(inicio)}).`,
    };
  }

  const fin = actividad.fechaFin ? actividad.fechaFin.split("T")[0] : "";
  if (fin && fechaIso > fin) {
    return {
      valida: false,
      motivo: `La actividad ya finalizó el ${formatearFechaLocal(fin)}.`,
    };
  }

  const dias = Array.isArray(actividad.dias)
    ? actividad.dias
    : actividad.dias instanceof Set
    ? Array.from(actividad.dias)
    : [];

  if (dias.length > 0) {
    const diaFecha = diaDeSemanaIso(fechaIso);
    if (diaFecha && !dias.includes(diaFecha)) {
      const diasDictado = dias
        .map((d) => NOMBRES_DIAS_ES[d] || d)
        .join(", ");
      if (fechaIso === hoyIso) {
        return {
          valida: false,
          motivo: `Hoy no es un día de dictado para esta actividad (se dicta: ${diasDictado}).`,
        };
      }
      const nombreDia = NOMBRES_DIAS_ES[diaFecha] || "este día";
      return {
        valida: false,
        motivo: `El día ${nombreDia} no corresponde a los días de dictado (se dicta: ${diasDictado}).`,
      };
    }
  }

  return { valida: true };
}

/** Pasa lo que devuelve el backend (lista plana de AsistenciaResponse) a las filas que muestra la planilla. */
export function filasDesdePlanilla(planilla) {
  const registros = Array.isArray(planilla) ? planilla : [];
  return registros.map((r) => ({
    socioId: r.socioId,
    nombre: r.socio ?? "",
    presente: Boolean(r.presente),
  }));
}

/** Lo que viaja al guardar: solo el socio y si estuvo. */
export function aMarcas(filas) {
  return filas.map(({ socioId, presente }) => ({ socioId, presente }));
}

export function contarPresentes(filas) {
  return filas.filter((f) => f.presente).length;
}

export function contarAusentes(filas) {
  return filas.length - contarPresentes(filas);
}

/** Porcentaje de asistencia, para el resumen de la planilla. */
export function porcentajePresentes(filas) {
  if (filas.length === 0) return 0;
  return Math.round((contarPresentes(filas) / filas.length) * 100);
}

/** Marca o desmarca a todos de una vez. */
export function marcarTodos(filas, presente) {
  return filas.map((f) => ({ ...f, presente }));
}

/** Cambia la marca de un solo alumno. */
export function alternar(filas, socioId) {
  return filas.map((f) => (f.socioId === socioId ? { ...f, presente: !f.presente } : f));
}
