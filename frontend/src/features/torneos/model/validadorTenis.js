/**
 * Validador puro de tanteadores de tenis según reglas oficiales RD14.
 * Partidos al mejor de 3 sets (gana quien obtenga 2 sets).
 */

function evaluarSet(token) {
  const partes = token.split("-");
  if (partes.length !== 2) return null;

  const g1 = parseInt(partes[0], 10);
  const g2 = parseInt(partes[1], 10);

  if (Number.isNaN(g1) || Number.isNaN(g2)) return null;

  const g1Gana = (g1 === 6 && g2 <= 4) || (g1 === 7 && (g2 === 5 || g2 === 6));
  const g2Gana = (g2 === 6 && g1 <= 4) || (g2 === 7 && (g1 === 5 || g1 === 6));

  if (g1Gana) return { ganador: 1, j1: g1, j2: g2 };
  if (g2Gana) return { ganador: 2, j1: g1, j2: g2 };

  return null;
}

export function validarTanteadorTenis(setsString) {
  if (!setsString || typeof setsString !== "string" || !setsString.trim()) {
    return { valido: false, motivo: "El tanteador no puede ser nulo ni vacío" };
  }

  const tokens = setsString.trim().split(/\s+/);
  if (tokens.length < 2 || tokens.length > 3) {
    return {
      valido: false,
      motivo: "Un partido al mejor de 3 sets debe constar de 2 o 3 sets",
    };
  }

  let setsGanados1 = 0;
  let setsGanados2 = 0;
  const setsParsed = [];

  for (const token of tokens) {
    if (setsGanados1 === 2 || setsGanados2 === 2) {
      return {
        valido: false,
        motivo: "El partido ya había finalizado en sets anteriores",
      };
    }

    const evaluacion = evaluarSet(token);
    if (!evaluacion) {
      return {
        valido: false,
        motivo: `Set inválido según reglas de tenis: ${token}`,
      };
    }

    if (evaluacion.ganador === 1) setsGanados1++;
    else setsGanados2++;

    setsParsed.push({ j1: evaluacion.j1, j2: evaluacion.j2 });
  }

  if (setsGanados1 !== 2 && setsGanados2 !== 2) {
    return {
      valido: false,
      motivo: "Partido inconcluso: ninguno de los participantes alcanzó 2 sets ganados",
    };
  }

  const ganador = setsGanados1 === 2 ? 1 : 2;

  return {
    valido: true,
    ganador,
    setsGanados1,
    setsGanados2,
    sets: setsParsed,
  };
}

export function formatearTanteador(sets) {
  if (!Array.isArray(sets) || sets.length === 0) return "";

  const formateados = [];
  for (const s of sets) {
    if (s && s.j1 !== "" && s.j2 !== "" && s.j1 !== undefined && s.j2 !== undefined && s.j1 !== null && s.j2 !== null) {
      formateados.push(`${s.j1}-${s.j2}`);
    } else {
      return "";
    }
  }

  return formateados.join(" ");
}
