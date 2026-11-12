import { describe, it, expect } from "vitest";
import {
  ESTADOS_RESERVA,
  validarFechaAntelacion,
  validarAcompaniantes,
  calcularHorasDuracion,
  formatearMoneda,
  formatearHorarioTurno,
  extraerMensajeError,
} from "../reserva";

describe("modelo de reserva", () => {
  it("valida que la fecha no sea pasada ni supere la antelación que informa el servidor", () => {
    const base = new Date("2026-10-20T10:00:00Z");
    expect(validarFechaAntelacion("2026-10-19", base, 30).valida).toBe(false);
    expect(validarFechaAntelacion("2026-10-25", base, 30).valida).toBe(true);
    expect(validarFechaAntelacion("2026-11-25", base, 30).valida).toBe(false);
    expect(validarFechaAntelacion("2026-11-25", base).valida).toBe(true);
  });

  it("valida acompañantes para SINGLE (exactamente 1) y DOBLES (exactamente 3)", () => {
    expect(validarAcompaniantes("SINGLE", []).valido).toBe(false);
    expect(validarAcompaniantes("SINGLE", [{ personaId: 10 }]).valido).toBe(true);
    expect(validarAcompaniantes("SINGLE", [{ personaId: 10 }, { personaId: 11 }]).valido).toBe(false);
    expect(validarAcompaniantes("DOBLES", [{ personaId: 1 }, { personaId: 2 }, { personaId: 3 }]).valido).toBe(true);
  });

  it("rechaza invitados sin nombre o apellido completo", () => {
    expect(validarAcompaniantes("SINGLE", [{ personaId: null, nombre: "", apellido: "Perez" }]).valido).toBe(false);
    expect(validarAcompaniantes("SINGLE", [{ personaId: null, nombre: "Juan", apellido: "Perez" }]).valido).toBe(true);
  });

  it("formatea moneda en pesos argentinos", () => {
    expect(formatearMoneda(8000)).toContain("8.000");
  });

  it("calcula la duración en horas entre horarios de inicio y fin", () => {
    expect(calcularHorasDuracion("18:00", "20:00")).toBe(2);
    expect(calcularHorasDuracion("18:00:00", "20:00:00")).toBe(2);
    expect(calcularHorasDuracion("08:00", "09:00")).toBe(1);
    expect(calcularHorasDuracion("10:00", "11:30")).toBe(1.5);
    expect(calcularHorasDuracion("20:00", "18:00")).toBe(0);
    expect(calcularHorasDuracion("", "18:00")).toBe(0);
  });

  it("formatea horario de turno a HH:mm - HH:mm", () => {
    expect(formatearHorarioTurno("18:00:00", "20:00:00")).toBe("18:00 - 20:00");
    expect(formatearHorarioTurno("08:00", "09:30")).toBe("08:00 - 09:30");
    expect(formatearHorarioTurno("", "")).toBe("—");
  });

  it("rechaza acompañantes socios duplicados o coincidentes con el titular", () => {
    expect(
      validarAcompaniantes("DOBLES", [
        { personaId: 10 },
        { personaId: 10 },
        { personaId: 11 },
      ]).valido
    ).toBe(false);

    expect(
      validarAcompaniantes(
        "SINGLE",
        [{ personaId: 5 }],
        { titularId: 5 }
      ).valido
    ).toBe(false);

    expect(
      validarAcompaniantes(
        "SINGLE",
        [{ personaId: 6 }],
        { titularId: 5 }
      ).valido
    ).toBe(true);
  });

  it("define estados con labels descriptivos y clases basadas en tokens semánticos", () => {
    expect(ESTADOS_RESERVA.PENDIENTE_PAGO.label).toBe("Turno retenido");
    expect(ESTADOS_RESERVA.PENDIENTE_PAGO.clase).toContain("var(--estado-aviso-");

    expect(ESTADOS_RESERVA.CONFIRMADA.label).toBe("Confirmada");
    expect(ESTADOS_RESERVA.CONFIRMADA.clase).toContain("var(--estado-ok-");

    expect(ESTADOS_RESERVA.EN_CURSO.label).toBe("En curso");
    expect(ESTADOS_RESERVA.EN_CURSO.clase).toContain("var(--estado-brand-");

    expect(ESTADOS_RESERVA.CANCELADA.label).toBe("Cancelada");
    expect(ESTADOS_RESERVA.CANCELADA.clase).toContain("var(--estado-error-");

    expect(ESTADOS_RESERVA.FINALIZADA.label).toBe("Finalizada");
    expect(ESTADOS_RESERVA.FINALIZADA.clase).toContain("var(--estado-neutro-");
  });

  describe("extraerMensajeError", () => {
    it("extrae el campo mensaje del ErrorResponse estándar", () => {
      const err = { response: { data: { mensaje: "Indicá el socio titular de la reserva" } } };
      expect(extraerMensajeError(err)).toBe("Indicá el socio titular de la reserva");
    });

    it("incluye el detalle de campos si está presente", () => {
      const err = {
        response: {
          data: {
            mensaje: "Hay datos que corregir",
            campos: { canchaId: "La cancha es requerida" },
          },
        },
      };
      expect(extraerMensajeError(err)).toBe("La cancha es requerida");
    });

    it("utiliza message o fallback si no hay mensaje en español", () => {
      expect(extraerMensajeError({ response: { data: { message: "Error en inglés" } } })).toBe("Error en inglés");
      expect(extraerMensajeError(new Error("Network Error"))).toBe("Network Error");
      expect(extraerMensajeError({}, "Fallback")).toBe("Fallback");
    });
  });
});
