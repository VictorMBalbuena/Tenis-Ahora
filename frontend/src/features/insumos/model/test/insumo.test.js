import { describe, expect, it } from "vitest";
import {
  EstadoStock,
  estadoDeStock,
  fechaCorta,
  porcentajeDisponible,
  sinStock,
  stockTotal,
} from "../insumo";

const insumo = (campos) => ({
  id: 1,
  nombre: "Raquetas",
  categoria: { id: 1, nombre: "Raqueta" },
  cantidadDisponible: 24,
  cantidadMinima: 6,
  enUso: 8,
  ...campos,
});

describe("modelo de insumo", () => {
  describe("stockTotal", () => {
    it("suma lo disponible más lo que está en uso", () => {
      expect(stockTotal(insumo())).toBe(32);
    });

    it("usa stockTotal si viene informado por el backend", () => {
      expect(stockTotal(insumo({ stockTotal: 30 }))).toBe(30);
    });

    it("no rompe si el backend no mandó enUso", () => {
      expect(stockTotal(insumo({ enUso: undefined }))).toBe(24);
    });
  });

  describe("estadoDeStock", () => {
    it("usa el estado que manda el backend", () => {
      expect(estadoDeStock(insumo({ estado: EstadoStock.STOCK_BAJO }))).toBe(
        EstadoStock.STOCK_BAJO,
      );
    });

    it("lo deduce si no viene: cero es sin stock", () => {
      expect(estadoDeStock(insumo({ cantidadDisponible: 0 }))).toBe(
        EstadoStock.SIN_STOCK,
      );
    });

    it("lo deduce si no viene: hasta el mínimo es stock bajo", () => {
      expect(
        estadoDeStock(insumo({ cantidadDisponible: 6, cantidadMinima: 6 })),
      ).toBe(EstadoStock.STOCK_BAJO);
    });

    it("lo deduce si no viene: por encima del mínimo está disponible", () => {
      expect(estadoDeStock(insumo())).toBe(EstadoStock.DISPONIBLE);
    });
  });

  describe("sinStock", () => {
    it("es verdadero solo cuando no queda nada", () => {
      expect(sinStock(insumo({ cantidadDisponible: 0 }))).toBe(true);
      expect(sinStock(insumo({ cantidadDisponible: 1 }))).toBe(false);
    });
  });

  describe("porcentajeDisponible", () => {
    it("calcula la proporción sobre el total", () => {
      expect(porcentajeDisponible(insumo())).toBe(75);
    });

    it("devuelve cero y no divide por cero cuando no hay nada", () => {
      expect(
        porcentajeDisponible(insumo({ cantidadDisponible: 0, enUso: 0 })),
      ).toBe(0);
    });
  });

  describe("fechaCorta", () => {
    it("formatea la fecha con hora que manda el backend", () => {
      expect(fechaCorta("2026-09-22T18:00:00")).toBe("22/09 18:00");
    });

    it("muestra un guión cuando no hay fecha", () => {
      expect(fechaCorta(null)).toBe("—");
    });
  });
});
