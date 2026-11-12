/**
 * Torneo o liga: la diferencia entre las dos formas de competencia (RF45 a RF50, RD14).
 *
 * La E05 las nombra siempre juntas ("torneo / liga") y comparten todo: inscripción, arancel, cobro y resultados.
 * Lo único que cambia es cómo se juegan, y por eso no hay un módulo de ligas aparte.
 */

export const TipoCompetencia = {
  TORNEO: "TORNEO",
  LIGA: "LIGA",
};

export const ETIQUETA_TIPO = {
  TORNEO: "Torneo",
  LIGA: "Liga",
};

export const COMO_SE_JUEGA = {
  TORNEO: "Eliminación directa",
  LIGA: "Todos contra todos",
};

export const AYUDA_TIPO = {
  TORNEO: "El que pierde queda afuera. Hace falta una cantidad de inscriptos que sea potencia de 2 (2, 4, 8, 16).",
  LIGA: "Cada uno juega una vez contra cada uno y nadie queda afuera. El campeón sale de la tabla de posiciones.",
};

export const OPCIONES_TIPO = [
  { value: TipoCompetencia.TORNEO, label: ETIQUETA_TIPO.TORNEO },
  { value: TipoCompetencia.LIGA, label: ETIQUETA_TIPO.LIGA },
];

/** Lo que no tiene tipo es un torneo: es lo único que existía antes de que se pudieran cargar ligas. */
export function tipoDe(competencia) {
  return competencia?.tipo ?? TipoCompetencia.TORNEO;
}

export function esLiga(competencia) {
  return tipoDe(competencia) === TipoCompetencia.LIGA;
}

export function etiquetaDeTipo(competencia) {
  return ETIQUETA_TIPO[tipoDe(competencia)] ?? tipoDe(competencia);
}

export function comoSeJuega(competencia) {
  return COMO_SE_JUEGA[tipoDe(competencia)] ?? "";
}

/** Cómo se llama a los participantes y al fixture en cada caso, para no decir "ronda" en una liga. */
export function vocabulario(competencia) {
  return esLiga(competencia)
    ? { fixture: "calendario", agrupador: "Fecha", singular: "liga" }
    : { fixture: "cuadro", agrupador: "Ronda", singular: "torneo" };
}

/**
 * Si la cantidad de inscriptos alcanza para armar el fixture, y si no, por qué.
 * Un torneo necesita una potencia de 2; una liga, dos o más y nada más.
 */
export function puedeGenerarFixture(competencia, cantidadInscriptos) {
  const n = Number(cantidadInscriptos) || 0;

  if (n < 2) {
    return { puede: false, motivo: "Hacen falta al menos 2 inscriptos." };
  }
  if (esLiga(competencia)) {
    return { puede: true, motivo: "" };
  }
  const esPotenciaDeDos = (n & (n - 1)) === 0;
  return esPotenciaDeDos
    ? { puede: true, motivo: "" }
    : {
        puede: false,
        motivo: `Un torneo por eliminación necesita una cantidad potencia de 2 (2, 4, 8, 16). Hay ${n} inscriptos.`,
      };
}

/** Cuántas fechas tiene una liga con esa cantidad de inscriptos (impar: uno descansa por fecha). */
export function fechasDeLiga(cantidadInscriptos) {
  const n = Number(cantidadInscriptos) || 0;
  if (n < 2) return 0;
  return (n % 2 === 0 ? n : n + 1) - 1;
}

/** Cuántos partidos se juegan en total en una liga: cada uno contra cada uno, una vez. */
export function partidosDeLiga(cantidadInscriptos) {
  const n = Number(cantidadInscriptos) || 0;
  return n < 2 ? 0 : (n * (n - 1)) / 2;
}

/** La diferencia que se muestra en la tabla, con el signo adelante. */
export function conSigno(numero) {
  const n = Number(numero) || 0;
  return n > 0 ? `+${n}` : String(n);
}
