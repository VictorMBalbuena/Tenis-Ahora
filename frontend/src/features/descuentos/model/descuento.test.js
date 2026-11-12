import { describe, expect, it } from "vitest";
import {
  FormaDeCalculo,
  TipoDescuento,
  beneficio,
  calcularSobre,
  etiqueta,
  formaDeCalculo,
  resumirDescuentos,
} from "./descuento";

const porPorcentaje = (campos) => ({
  id: 1,
  tipo: TipoDescuento.LIGA_REGULAR,
  porcentaje: 15,
  montoFijo: null,
  condiciones: "Equipos de liga regular",
  activo: true,
  ...campos,
});

const porMonto = (campos) => ({
  id: 2,
  tipo: TipoDescuento.ESCUELA_AFILIADA,
  porcentaje: null,
  montoFijo: 4000,
  condiciones: "Alumnos de escuelas afiliadas",
  activo: true,
  ...campos,
});

describe("modelo de descuento", () => {
  describe("formaDeCalculo", () => {
    it("es porcentaje cuando vino el porcentaje", () => {
      expect(formaDeCalculo(porPorcentaje())).toBe(FormaDeCalculo.PORCENTAJE);
    });

    it("es monto fijo cuando vino el monto", () => {
      expect(formaDeCalculo(porMonto())).toBe(FormaDeCalculo.MONTO_FIJO);
    });

    it("es nula si no vino ninguno, porque la entidad lo permite", () => {
      expect(
        formaDeCalculo(porPorcentaje({ porcentaje: null, montoFijo: null })),
      ).toBeNull();
    });

    it("prioriza el porcentaje si por error vinieran los dos", () => {
      expect(formaDeCalculo(porPorcentaje({ montoFijo: 500 }))).toBe(
        FormaDeCalculo.PORCENTAJE,
      );
    });

    it("toma el cero como un valor cargado, no como ausencia", () => {
      expect(formaDeCalculo(porPorcentaje({ porcentaje: 0 }))).toBe(
        FormaDeCalculo.PORCENTAJE,
      );
    });
  });

  describe("beneficio", () => {
    it("escribe el porcentaje con su signo", () => {
      expect(beneficio(porPorcentaje())).toBe("15 %");
    });

    it("escribe el monto fijo como plata", () => {
      expect(beneficio(porMonto())).toContain("4.000");
    });

    it("avisa cuando no hay beneficio cargado", () => {
      expect(
        beneficio(porPorcentaje({ porcentaje: null, montoFijo: null })),
      ).toBe("Sin definir");
    });
  });

  describe("calcularSobre", () => {
    it("aplica el porcentaje sobre el monto", () => {
      expect(calcularSobre(porPorcentaje(), 24000)).toBe(3600);
    });

    it("aplica el monto fijo tal cual", () => {
      expect(calcularSobre(porMonto(), 24000)).toBe(4000);
    });

    it("nunca descuenta mas que el total: no existe un cobro negativo", () => {
      expect(calcularSobre(porMonto(), 1000)).toBe(1000);
    });

    it("no descuenta nada si no hay beneficio cargado", () => {
      expect(
        calcularSobre(porPorcentaje({ porcentaje: null, montoFijo: null }), 5000),
      ).toBe(0);
    });

    it("trata un monto invalido como cero", () => {
      expect(calcularSobre(porPorcentaje(), "")).toBe(0);
    });

    it("redondea a dos decimales", () => {
      expect(calcularSobre(porPorcentaje({ porcentaje: 33 }), 100.5)).toBe(
        33.17,
      );
    });
  });

  describe("etiqueta", () => {
    it("usa el tipo, porque la entidad no tiene nombre", () => {
      expect(etiqueta(porPorcentaje())).toBe("Liga regular");
    });

    it("muestra el valor crudo si apareciera un tipo desconocido", () => {
      expect(etiqueta({ tipo: "NUEVO_TIPO" })).toBe("NUEVO_TIPO");
    });
  });

  describe("resumirDescuentos", () => {
    const lista = [
      porPorcentaje(),
      porMonto({ activo: false }),
      porPorcentaje({ id: 3, porcentaje: null, montoFijo: null }),
    ];

    it("cuenta activos e inactivos", () => {
      const r = resumirDescuentos(lista);
      expect(r.activos).toBe(2);
      expect(r.inactivos).toBe(1);
    });

    it("cuenta los que quedaron sin beneficio cargado", () => {
      expect(resumirDescuentos(lista).sinBeneficio).toBe(1);
    });

    it("toma el total del backend si se lo pasan", () => {
      expect(resumirDescuentos(lista, 12).total).toBe(12);
    });
  });
});
