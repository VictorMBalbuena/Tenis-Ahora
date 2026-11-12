import { describe, expect, it } from "vitest";
import {
  formatearTanteador,
  validarTanteadorTenis,
} from "../validadorTenis";

describe("validadorTenis (RD14)", () => {
  describe("validarTanteadorTenis", () => {
    it("valida resultado estándar en dos sets (2-0)", () => {
      const res = validarTanteadorTenis("6-4 6-3");
      expect(res.valido).toBe(true);
      expect(res.ganador).toBe(1);
      expect(res.sets).toEqual([
        { j1: 6, j2: 4 },
        { j1: 6, j2: 3 },
      ]);
    });

    it("valida resultado estándar a favor del participante 2 (0-2)", () => {
      const res = validarTanteadorTenis("3-6 4-6");
      expect(res.valido).toBe(true);
      expect(res.ganador).toBe(2);
      expect(res.sets).toEqual([
        { j1: 3, j2: 6 },
        { j1: 4, j2: 6 },
      ]);
    });

    it("valida resultado con tres sets y tie-breaks (2-1)", () => {
      const res = validarTanteadorTenis("7-6 3-6 7-6");
      expect(res.valido).toBe(true);
      expect(res.ganador).toBe(1);
      expect(res.sets?.length).toBe(3);
    });

    it("valida set con ventaja 7-5", () => {
      const res = validarTanteadorTenis("7-5 6-2");
      expect(res.valido).toBe(true);
      expect(res.ganador).toBe(1);
    });

    it("valida tercer set cuando hay empate 1-1", () => {
      const res = validarTanteadorTenis("6-4 3-6 6-2");
      expect(res.valido).toBe(true);
      expect(res.ganador).toBe(1);
    });

    it("rechaza tanteadores nulos o vacíos", () => {
      expect(validarTanteadorTenis(null).valido).toBe(false);
      expect(validarTanteadorTenis("").valido).toBe(false);
      expect(validarTanteadorTenis("   ").valido).toBe(false);
    });

    it("rechaza resultado con menos de 2 sets", () => {
      const res = validarTanteadorTenis("6-4");
      expect(res.valido).toBe(false);
      expect(res.motivo).toMatch(/2 o 3 sets/i);
    });

    it("rechaza sets imposibles como 6-5 o 9-2", () => {
      expect(validarTanteadorTenis("6-5 6-3").valido).toBe(false);
      expect(validarTanteadorTenis("9-2 6-3").valido).toBe(false);
    });

    it("rechaza partido inconcluso (1-1 con 2 sets)", () => {
      const res = validarTanteadorTenis("6-4 4-6");
      expect(res.valido).toBe(false);
      expect(res.motivo).toMatch(/inconcluso/i);
    });

    it("rechaza set extra cuando un participante ya ganó 2-0", () => {
      const res = validarTanteadorTenis("6-4 6-3 6-2");
      expect(res.valido).toBe(false);
      expect(res.motivo).toMatch(/finalizado/i);
    });

    it("rechaza formato de texto no numérico", () => {
      const res = validarTanteadorTenis("seis-cuatro seis-tres");
      expect(res.valido).toBe(false);
    });
  });

  describe("formatearTanteador", () => {
    it("convierte array de sets a string formateado", () => {
      const sets = [
        { j1: 6, j2: 4 },
        { j1: 3, j2: 6 },
        { j1: 7, j2: 5 },
      ];
      expect(formatearTanteador(sets)).toBe("6-4 3-6 7-5");
    });

    it("ignora sets incompletos o vacíos", () => {
      expect(formatearTanteador([])).toBe("");
      expect(formatearTanteador(null)).toBe("");
      expect(formatearTanteador([{ j1: 6, j2: "" }])).toBe("");
    });
  });
});
