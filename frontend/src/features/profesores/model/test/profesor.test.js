import { describe, expect, it } from "vitest";
import {
  EstadoValidacion,
  direccionCompleta,
  dniFormateado,
  estaValidado,
  fechaCorta,
  nombreCompleto,
  puedeDictarClases,
  telefonoCompleto,
  validarProfesor,
} from "../profesor";

const profesor = (campos) => ({
  id: 1,
  nombre: "Bruno",
  apellido: "Tealdi",
  dni: "44966422",
  calle: "Av. Rivadavia",
  numeroCalle: "4820",
  codigoArea: "11",
  numeroTelefono: "2278-6604",
  email: "bruno.tealdi@gmail.com",
  activo: true,
  estadoValidacion: EstadoValidacion.VALIDADO,
  tituloHabilitante: "Profesor Nacional de Tenis",
  ...campos,
});

describe("modelo de profesor", () => {
  describe("nombreCompleto", () => {
    it("une nombre y apellido", () => {
      expect(nombreCompleto(profesor())).toBe("Bruno Tealdi");
    });
  });

  describe("estaValidado", () => {
    it("distingue validado de pendiente", () => {
      expect(estaValidado(profesor())).toBe(true);
      expect(
        estaValidado(profesor({ estadoValidacion: EstadoValidacion.PENDIENTE })),
      ).toBe(false);
    });
  });

  describe("puedeDictarClases", () => {
    it("necesita estar activo y validado a la vez", () => {
      expect(puedeDictarClases(profesor())).toBe(true);
    });

    it("no puede si la cuenta esta inactiva, aunque este validado", () => {
      expect(puedeDictarClases(profesor({ activo: false }))).toBe(false);
    });

    it("no puede si la certificacion esta pendiente, aunque este activo", () => {
      expect(
        puedeDictarClases(
          profesor({ estadoValidacion: EstadoValidacion.PENDIENTE }),
        ),
      ).toBe(false);
    });
  });

  describe("direccionCompleta", () => {
    it("une calle y numero, que en la entidad son campos distintos", () => {
      expect(direccionCompleta(profesor())).toBe("Av. Rivadavia 4820");
    });

    it("muestra solo la calle si no hay numero", () => {
      expect(direccionCompleta(profesor({ numeroCalle: "" }))).toBe(
        "Av. Rivadavia",
      );
    });

    it("muestra un guion si no hay direccion", () => {
      expect(direccionCompleta(profesor({ calle: "", numeroCalle: "" }))).toBe(
        "—",
      );
    });
  });

  describe("telefonoCompleto", () => {
    it("une codigo de area y numero", () => {
      expect(telefonoCompleto(profesor())).toBe("11 2278-6604");
    });

    it("muestra el numero solo si no hay codigo de area", () => {
      expect(telefonoCompleto(profesor({ codigoArea: "" }))).toBe("2278-6604");
    });

    it("muestra un guion si no hay telefono", () => {
      expect(telefonoCompleto(profesor({ numeroTelefono: "" }))).toBe("—");
    });
  });

  describe("dniFormateado", () => {
    it("agrega los puntos de a tres digitos", () => {
      expect(dniFormateado("44966422")).toBe("44.966.422");
    });

    it("devuelve vacio si no hay dni", () => {
      expect(dniFormateado(null)).toBe("");
    });
  });

  describe("fechaCorta", () => {
    it("pasa la fecha ISO a formato local", () => {
      expect(fechaCorta("2026-04-18")).toBe("18/04/2026");
    });

    it("ignora la hora si el backend la manda", () => {
      expect(fechaCorta("2026-04-18T10:30:00")).toBe("18/04/2026");
    });

    it("muestra un guion si no hay fecha", () => {
      expect(fechaCorta(null)).toBe("—");
    });
  });

  describe("validarProfesor", () => {
    const datosValidos = () => ({
      nombre: "Ana",
      apellido: "Gómez",
      dni: "30123456",
      email: "ana@correo.com",
      numeroTelefono: "12345678",
      tituloHabilitante: "Profesor Nacional de Tenis",
      password: "Clave-segura-123",
      numeroCertificacion: "CERT-001",
      entidadEmisora: "FAT",
      fechaEmision: "2024-01-01",
    });

    it("sin errores con datos completos en el alta", () => {
      expect(validarProfesor(datosValidos(), { esAlta: true })).toEqual({});
    });

    it("exige contraseña y certificación solo en el alta", () => {
      const sinCredenciales = { ...datosValidos(), password: "", numeroCertificacion: "" };
      const errores = validarProfesor(sinCredenciales, { esAlta: true });
      expect(errores.password).toBeDefined();
      expect(errores.numeroCertificacion).toBeDefined();
    });

    it("no exige contraseña ni certificación al modificar", () => {
      const datos = { ...datosValidos(), password: "", numeroCertificacion: "", entidadEmisora: "", fechaEmision: "" };
      expect(validarProfesor(datos, { esAlta: false })).toEqual({});
    });

    it("rechaza un DNI que no tiene 7 u 8 dígitos", () => {
      const errores = validarProfesor({ ...datosValidos(), dni: "123" }, { esAlta: true });
      expect(errores.dni).toBeDefined();
    });

    it("rechaza un correo sin formato válido", () => {
      const errores = validarProfesor({ ...datosValidos(), email: "no-es-un-correo" }, { esAlta: true });
      expect(errores.email).toBeDefined();
    });

    it("rechaza una certificación emitida después de hoy", () => {
      // Un título no se puede emitir mañana. El calendario ya corta en hoy, pero la fecha
      // se puede escribir a mano.
      const maniana = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);
      const errores = validarProfesor({ ...datosValidos(), fechaEmision: maniana }, { esAlta: true });
      expect(errores.fechaEmision).toBeDefined();
    });

    it("acepta una certificación emitida hoy", () => {
      const hoy = new Date().toISOString().slice(0, 10);
      const errores = validarProfesor({ ...datosValidos(), fechaEmision: hoy }, { esAlta: true });
      expect(errores.fechaEmision).toBeUndefined();
    });

    it("exige el título habilitante siempre", () => {
      const errores = validarProfesor({ ...datosValidos(), tituloHabilitante: "" }, { esAlta: false });
      expect(errores.tituloHabilitante).toBeDefined();
    });
  });
});
