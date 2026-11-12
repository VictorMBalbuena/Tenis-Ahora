import { describe, expect, it } from "vitest";
import { conceptoLegible, esEditable, estaAnulado, medioLegible, numero, resumirRecibos } from "../recibo";

const recibo = {
  id: 9,
  numeroRecibo: "REC-0000009",
  monto: 18000,
  concepto: "Saldo del alquiler",
  fechaEmision: "2026-10-27T18:30:00",
  cobro: { id: 4, concepto: "SALDO", medioPago: "QR" },
};

describe("numero", () => {
  it("usa el número correlativo del backend", () => {
    expect(numero(recibo)).toBe("REC-0000009");
  });

  it("si todavía no tiene número, cae al id", () => {
    expect(numero({ id: 3 })).toBe("#3");
  });
});

describe("textos legibles", () => {
  it("prefiere el concepto propio del recibo", () => {
    expect(conceptoLegible(recibo)).toBe("Saldo del alquiler");
  });

  it("si el recibo no tiene concepto, usa el del cobro traducido", () => {
    expect(conceptoLegible({ cobro: { concepto: "INSCRIPCION" } })).toBe("Inscripción");
  });

  it("el medio de pago vive en el cobro, no en el recibo", () => {
    expect(medioLegible(recibo)).toBe("Mercado Pago (QR)");
    expect(medioLegible({})).toBe("—");
  });
});

describe("anulación", () => {
  it("un recibo vigente admite acciones (anular)", () => {
    expect(estaAnulado(recibo)).toBe(false);
    expect(esEditable(recibo)).toBe(true);
  });

  it("un recibo anulado ya no admite acciones", () => {
    const anulado = { ...recibo, estado: "ANULADO" };
    expect(estaAnulado(anulado)).toBe(true);
    expect(esEditable(anulado)).toBe(false);
  });
});

describe("resumirRecibos", () => {
  it("cuenta vigentes y anulados, y suma solo los vigentes", () => {
    const resumen = resumirRecibos(
      [
        { monto: 18000 },
        { monto: 9000 },
        { monto: 4000, estado: "ANULADO" },
      ],
      3,
    );
    expect(resumen).toEqual({ total: 3, vigentes: 2, anulados: 1, monto: 27000 });
  });

  it("usa el total del backend, no el de la página", () => {
    expect(resumirRecibos([{ monto: 1000 }], 42).total).toBe(42);
  });
});
