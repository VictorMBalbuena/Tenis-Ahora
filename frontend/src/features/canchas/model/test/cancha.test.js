import { describe, expect, it } from "vitest";
import { determinarEstado, etiquetaSuperficie } from "../cancha";

describe("modelo de cancha", () => {
  describe("determinarEstado", () => {
    it("es Mantenimiento si tiene un bloqueo vigente, sin importar activa", () => {
      expect(determinarEstado({ enMantenimiento: true, activa: true })).toBe("Mantenimiento");
      expect(determinarEstado({ enMantenimiento: true, activa: false })).toBe("Mantenimiento");
    });

    it("es Disponible si está activa y sin bloqueo", () => {
      expect(determinarEstado({ enMantenimiento: false, activa: true })).toBe("Disponible");
    });

    it("es Deshabilitada si no está activa y sin bloqueo", () => {
      expect(determinarEstado({ enMantenimiento: false, activa: false })).toBe("Deshabilitada");
    });
  });

  describe("etiquetaSuperficie", () => {
    it("traduce el enum del backend a la etiqueta en español", () => {
      expect(etiquetaSuperficie("LADRILLO")).toBe("Ladrillo");
      expect(etiquetaSuperficie("CEMENTO")).toBe("Cemento");
      expect(etiquetaSuperficie("PASTO")).toBe("Pasto");
    });

    it("devuelve el valor tal cual si no hay etiqueta mapeada", () => {
      expect(etiquetaSuperficie("OTRA")).toBe("OTRA");
    });
  });
});
