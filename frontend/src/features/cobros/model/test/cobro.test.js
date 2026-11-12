import { describe, expect, it } from "vitest";
import {
  ConceptoCobro,
  EstadoCobro,
  MedioPago,
  Origen,
  bonificacionDe,
  descripcionOrigen,
  desgloseDeCobro,
  desglosar,
  esAnulable,
  esReembolso,
  etiquetaDescuento,
  etiquetaReserva,
  etiquetaTorneo,
  fechaHora,
  montoConSigno,
  origenDe,
  rangoDeFechasValido,
  resumirCobros,
  validarCobro,
} from "../cobro";

const porDebito = {
  reservaId: "7",
  medioPago: MedioPago.DEBITO,
  numeroOperacion: "000123",
  cuotas: "",
  aprobado: true,
};

describe("validarCobro", () => {
  it("sin errores con datos válidos", () => {
    expect(validarCobro(porDebito)).toEqual({});
  });

  it("exige la reserva y el medio de pago", () => {
    const errores = validarCobro({ ...porDebito, reservaId: "", medioPago: "" });
    expect(errores.reservaId).toBeDefined();
    expect(errores.medioPago).toBeDefined();
  });

  it("exige el número de operación para débito y crédito", () => {
    expect(validarCobro({ ...porDebito, numeroOperacion: "" }).numeroOperacion).toBeDefined();
    expect(validarCobro({ ...porDebito, medioPago: MedioPago.CREDITO, numeroOperacion: "" }).numeroOperacion).toBeDefined();
  });

  it("el número de operación solo admite números, hasta 20 dígitos", () => {
    expect(validarCobro({ ...porDebito, numeroOperacion: "ABC123" }).numeroOperacion).toBeDefined();
    expect(validarCobro({ ...porDebito, numeroOperacion: "1".repeat(21) }).numeroOperacion).toBeDefined();
    expect(validarCobro({ ...porDebito, numeroOperacion: "1".repeat(20) }).numeroOperacion).toBeUndefined();
  });

  it("el QR no necesita número de operación", () => {
    const errores = validarCobro({ ...porDebito, medioPago: MedioPago.QR, numeroOperacion: "" });
    expect(errores.numeroOperacion).toBeUndefined();
  });

  it("rechaza cuotas fuera de 1 a 24, solo si se indicaron", () => {
    const base = { ...porDebito, medioPago: MedioPago.CREDITO, numeroOperacion: "000123" };
    expect(validarCobro({ ...base, cuotas: "" }).cuotas).toBeUndefined();
    expect(validarCobro({ ...base, cuotas: "6" }).cuotas).toBeUndefined();
    expect(validarCobro({ ...base, cuotas: "0" }).cuotas).toBeDefined();
    expect(validarCobro({ ...base, cuotas: "25" }).cuotas).toBeDefined();
  });
});

describe("bonificacionDe", () => {
  it("calcula el porcentaje sobre el monto", () => {
    expect(bonificacionDe({ porcentaje: 15 }, 20000)).toBe(3000);
  });

  it("toma el monto fijo tal cual", () => {
    expect(bonificacionDe({ montoFijo: 2500 }, 20000)).toBe(2500);
  });

  it("un monto fijo mayor que el total no deja el cobro en negativo", () => {
    expect(bonificacionDe({ montoFijo: 50000 }, 20000)).toBe(20000);
  });

  it("un descuento sin porcentaje ni monto fijo no bonifica nada", () => {
    expect(bonificacionDe({}, 20000)).toBe(0);
  });
});

describe("desglosar", () => {
  it("sin descuentos, el total es el monto original", () => {
    const desglose = desglosar(18000);
    expect(desglose).toMatchObject({ original: 18000, bonificado: 0, total: 18000, hayDescuento: false });
  });

  it("muestra original, descuento y total, que es lo que pide el ticket", () => {
    const desglose = desglosar(20000, [{ id: 1, porcentaje: 10 }]);
    expect(desglose.original).toBe(20000);
    expect(desglose.bonificado).toBe(2000);
    expect(desglose.total).toBe(18000);
    expect(desglose.hayDescuento).toBe(true);
  });

  it("acumula los descuentos sobre el monto original, así el orden no cambia el total", () => {
    const a = { id: 1, porcentaje: 10 };
    const b = { id: 2, montoFijo: 1000 };
    expect(desglosar(20000, [a, b]).total).toBe(desglosar(20000, [b, a]).total);
    expect(desglosar(20000, [a, b]).total).toBe(17000);
  });

  it("los descuentos nunca dejan un total negativo", () => {
    const desglose = desglosar(5000, [{ id: 1, porcentaje: 80 }, { id: 2, montoFijo: 4000 }]);
    expect(desglose.total).toBe(0);
    expect(desglose.bonificado).toBe(5000);
  });

  it("lleva el detalle de cuánto bonificó cada descuento", () => {
    const desglose = desglosar(10000, [{ id: 1, porcentaje: 25 }]);
    expect(desglose.aplicados).toHaveLength(1);
    expect(desglose.aplicados[0].bonificacion).toBe(2500);
  });
});

describe("desgloseDeCobro", () => {
  it("toma el descuento de la diferencia entre el monto base y el final", () => {
    expect(desgloseDeCobro({ montoBase: 20000, montoFinal: 18000 })).toMatchObject({
      original: 20000,
      bonificado: 2000,
      total: 18000,
      hayDescuento: true,
    });
  });

  it("sin diferencia no hay descuento", () => {
    expect(desgloseDeCobro({ montoBase: 18000, montoFinal: 18000 }).hayDescuento).toBe(false);
  });

  it("sin monto base, el final es el original", () => {
    expect(desgloseDeCobro({ montoFinal: 18000 })).toMatchObject({ original: 18000, total: 18000, hayDescuento: false });
  });

  it("un descuento del 100% deja el total en cero", () => {
    expect(desgloseDeCobro({ montoBase: 18000, montoFinal: 0 })).toMatchObject({ bonificado: 18000, total: 0, hayDescuento: true });
  });

  it("no inventa un descuento negativo", () => {
    expect(desgloseDeCobro({ montoBase: 100, montoFinal: 150 }).hayDescuento).toBe(false);
  });

  it("no arrastra errores de redondeo", () => {
    expect(desgloseDeCobro({ montoBase: 100.1, montoFinal: 90.05 }).bonificado).toBe(10.05);
  });
});

describe("rangoDeFechasValido", () => {
  it("rechaza solo un desde posterior al hasta", () => {
    expect(rangoDeFechasValido("2026-11-08", "2026-11-07")).toBe(false);
    expect(rangoDeFechasValido("2026-11-07", "2026-11-07")).toBe(true);
    expect(rangoDeFechasValido("2026-11-08", "")).toBe(true);
    expect(rangoDeFechasValido("", "2026-11-07")).toBe(true);
  });
});

describe("esAnulable", () => {
  it("solo se anula lo pendiente o pagado", () => {
    expect(esAnulable({ estado: EstadoCobro.PENDIENTE })).toBe(true);
    expect(esAnulable({ estado: EstadoCobro.PAGADO })).toBe(true);
    expect(esAnulable({ estado: EstadoCobro.RECHAZADO })).toBe(false);
    expect(esAnulable({ estado: EstadoCobro.VENCIDO })).toBe(false);
    expect(esAnulable({ estado: EstadoCobro.ANULADO })).toBe(false);
  });
});

describe("origen del cobro", () => {
  it("lo deduce de cuál de las dos referencias trae", () => {
    expect(origenDe({ reserva: { id: 3 } })).toBe(Origen.RESERVA);
    expect(origenDe({ torneo: { id: 4 } })).toBe(Origen.TORNEO);
    expect(origenDe({})).toBeNull();
  });

  it("describe la reserva con su cancha y su día", () => {
    const texto = descripcionOrigen({
      reserva: { id: 12, cancha: { identificador: "Cancha 3" }, fecha: "2026-11-02", horaInicio: "18:00:00" },
    });
    expect(texto).toContain("Cancha 3");
    expect(texto).toContain("02/11/2026");
    expect(texto).toContain("18:00");
  });

  it("nombra el torneo con su categoría y modalidad, porque no tiene nombre propio", () => {
    expect(etiquetaTorneo({ id: 5, categoria: "FEMENINO", modalidad: "PAREJA" })).toBe("Torneo #5 (femenino, pareja)");
  });

  it("aguanta una reserva o un torneo sin datos", () => {
    expect(etiquetaReserva(null)).toBe("—");
    expect(etiquetaTorneo(null)).toBe("—");
    expect(descripcionOrigen({})).toBe("—");
  });
});

describe("reembolsos", () => {
  it("un reembolso sale de la caja", () => {
    expect(esReembolso({ concepto: ConceptoCobro.REEMBOLSO })).toBe(true);
    expect(montoConSigno({ concepto: ConceptoCobro.REEMBOLSO, montoFinal: 5000 })).toBe(-5000);
  });

  it("el resto de los conceptos entra", () => {
    expect(montoConSigno({ concepto: ConceptoCobro.SALDO, montoFinal: 5000 })).toBe(5000);
  });
});

describe("resumirCobros", () => {
  it("separa ingresos de reembolsos y calcula el neto", () => {
    const resumen = resumirCobros(
      [
        { concepto: ConceptoCobro.SALDO, estado: EstadoCobro.PAGADO, montoFinal: 18000 },
        { concepto: ConceptoCobro.SENA, estado: EstadoCobro.PAGADO, montoFinal: 9000 },
        { concepto: ConceptoCobro.REEMBOLSO, estado: EstadoCobro.PAGADO, montoFinal: 4000 },
      ],
      3,
    );
    expect(resumen).toEqual({ total: 3, ingresos: 27000, reembolsos: 4000, neto: 23000 });
  });

  it("no suma lo pendiente, rechazado, vencido ni anulado", () => {
    const sinPagar = [EstadoCobro.PENDIENTE, EstadoCobro.RECHAZADO, EstadoCobro.VENCIDO, EstadoCobro.ANULADO].map((estado) => ({
      concepto: ConceptoCobro.SENA,
      estado,
      montoFinal: 9000,
    }));
    expect(resumirCobros(sinPagar).ingresos).toBe(0);
  });

  it("usa el total del backend, no el de la página", () => {
    expect(resumirCobros([{ concepto: ConceptoCobro.SALDO, montoFinal: 1000 }], 97).total).toBe(97);
  });
});

describe("etiquetaDescuento", () => {
  it("nombra el tipo y cuánto bonifica", () => {
    expect(etiquetaDescuento({ tipo: "PAQUETE_HORAS", porcentaje: 10 })).toBe("Paquete de horas (10%)");
    expect(etiquetaDescuento({ tipo: "LIGA_REGULAR", montoFijo: 1500 })).toContain("Liga regular");
  });
});

describe("fechaHora", () => {
  it("pasa el ISO del backend a formato local", () => {
    expect(fechaHora("2026-10-27T18:30:00")).toBe("27/10/2026 18:30");
  });

  it("sin fecha devuelve un guión", () => {
    expect(fechaHora(null)).toBe("—");
  });
});
