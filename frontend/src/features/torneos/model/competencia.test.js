import { describe, expect, it } from "vitest";
import {
  comoSeJuega,
  conSigno,
  esLiga,
  etiquetaDeTipo,
  fechasDeLiga,
  partidosDeLiga,
  puedeGenerarFixture,
  tipoDe,
  vocabulario,
} from "./competencia";

const torneo = { tipo: "TORNEO" };
const liga = { tipo: "LIGA" };

describe("tipo de competencia", () => {
  it("una competencia sin tipo es un torneo: es lo único que existía antes", () => {
    expect(tipoDe({})).toBe("TORNEO");
    expect(tipoDe(null)).toBe("TORNEO");
    expect(esLiga({})).toBe(false);
  });

  it("reconoce una liga", () => {
    expect(esLiga(liga)).toBe(true);
    expect(etiquetaDeTipo(liga)).toBe("Liga");
    expect(comoSeJuega(liga)).toBe("Todos contra todos");
  });

  it("reconoce un torneo", () => {
    expect(etiquetaDeTipo(torneo)).toBe("Torneo");
    expect(comoSeJuega(torneo)).toBe("Eliminación directa");
  });

  it("cada uno usa su propio vocabulario, para no decir 'ronda' en una liga", () => {
    expect(vocabulario(liga).agrupador).toBe("Fecha");
    expect(vocabulario(torneo).agrupador).toBe("Ronda");
  });
});

describe("cuándo se puede generar el fixture", () => {
  it("un torneo necesita una potencia de 2", () => {
    expect(puedeGenerarFixture(torneo, 2).puede).toBe(true);
    expect(puedeGenerarFixture(torneo, 4).puede).toBe(true);
    expect(puedeGenerarFixture(torneo, 8).puede).toBe(true);
    expect(puedeGenerarFixture(torneo, 16).puede).toBe(true);
  });

  it("un torneo con una cantidad que no es potencia de 2 avisa por qué no", () => {
    const resultado = puedeGenerarFixture(torneo, 5);

    expect(resultado.puede).toBe(false);
    expect(resultado.motivo).toContain("potencia de 2");
    expect(resultado.motivo).toContain("5 inscriptos");
  });

  it("una liga sirve con cualquier cantidad desde 2", () => {
    [2, 3, 5, 7, 9, 13].forEach((n) => {
      expect(puedeGenerarFixture(liga, n).puede).toBe(true);
    });
  });

  it("con menos de 2 no se puede armar nada, ni torneo ni liga", () => {
    expect(puedeGenerarFixture(torneo, 1).puede).toBe(false);
    expect(puedeGenerarFixture(liga, 1).puede).toBe(false);
    expect(puedeGenerarFixture(liga, 0).motivo).toContain("al menos 2");
  });
});

describe("cuentas de una liga", () => {
  it("con cantidad par hay n-1 fechas y nadie descansa", () => {
    expect(fechasDeLiga(4)).toBe(3);
    expect(fechasDeLiga(8)).toBe(7);
  });

  it("con cantidad impar hay n fechas, porque uno descansa cada vez", () => {
    expect(fechasDeLiga(5)).toBe(5);
    expect(fechasDeLiga(7)).toBe(7);
  });

  it("los partidos son cada uno contra cada uno, una sola vez", () => {
    expect(partidosDeLiga(2)).toBe(1);
    expect(partidosDeLiga(4)).toBe(6);
    expect(partidosDeLiga(5)).toBe(10);
    expect(partidosDeLiga(8)).toBe(28);
  });

  it("con menos de dos no hay liga", () => {
    expect(fechasDeLiga(1)).toBe(0);
    expect(partidosDeLiga(1)).toBe(0);
  });
});

describe("diferencias de la tabla", () => {
  it("una diferencia positiva lleva el signo adelante", () => {
    expect(conSigno(3)).toBe("+3");
  });

  it("una negativa o el cero se muestran tal cual", () => {
    expect(conSigno(-2)).toBe("-2");
    expect(conSigno(0)).toBe("0");
  });
});
