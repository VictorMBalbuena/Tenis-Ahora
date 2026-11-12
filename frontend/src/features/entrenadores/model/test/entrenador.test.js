import { describe, expect, it } from "vitest";
import { mapearErroresBackend, validarEntrenador } from "../entrenador";

const datosValidos = {
  nombre: "Lucas",
  apellido: "Pérez",
  dni: "31123456",
  email: "lucas@correo.com",
  numeroTelefono: "22786604",
  certificacionDeportiva: "Entrenador Nacional de Tenis",
  password: "Clave-segura-123",
  numeroCertificacion: "PF-2023-0092",
  entidadEmisora: "ISEF",
  fechaEmision: "2023-04-18",
};

describe("validarEntrenador", () => {
  it("sin errores con datos válidos en el alta", () => {
    expect(validarEntrenador(datosValidos, { esAlta: true })).toEqual({});
  });

  it("rechaza una certificación emitida después de hoy", () => {
    // Un título no se puede emitir mañana. El calendario ya corta en hoy, pero la fecha
    // se puede escribir a mano.
    const maniana = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);
    expect(validarEntrenador({ ...datosValidos, fechaEmision: maniana }, { esAlta: true }).fechaEmision).toBeDefined();
  });

  it("acepta una certificación emitida hoy", () => {
    const hoy = new Date().toISOString().slice(0, 10);
    expect(validarEntrenador({ ...datosValidos, fechaEmision: hoy }, { esAlta: true }).fechaEmision).toBeUndefined();
  });

  it("en el alta, exige contraseña y certificación", () => {
    const errores = validarEntrenador({ ...datosValidos, password: "", numeroCertificacion: "" }, { esAlta: true });
    expect(errores.password).toBeDefined();
    expect(errores.numeroCertificacion).toBeDefined();
  });

  it("al editar, no exige contraseña ni certificación", () => {
    const errores = validarEntrenador({ ...datosValidos, password: "", numeroCertificacion: "" }, { esAlta: false });
    expect(errores).toEqual({});
  });

  it("rechaza un DNI inválido", () => {
    expect(validarEntrenador({ ...datosValidos, dni: "abc" }, { esAlta: true }).dni).toBeDefined();
  });

  it("rechaza un correo inválido", () => {
    expect(validarEntrenador({ ...datosValidos, email: "no-es-un-correo" }, { esAlta: true }).email).toBeDefined();
  });

  it("exige la certificación deportiva siempre", () => {
    expect(validarEntrenador({ ...datosValidos, certificacionDeportiva: "" }, { esAlta: false }).certificacionDeportiva).toBeDefined();
  });
});

describe("mapearErroresBackend", () => {
  it("aplana campos anidados de VALIDACION_FALLIDA", () => {
    const error = {
      response: {
        data: {
          codigo: "VALIDACION_FALLIDA",
          campos: {
            "certificacion.fechaVencimiento": "La certificación está vencida",
            fechaNacimiento: "La fecha de nacimiento debe ser pasada",
          },
        },
      },
    };
    const errores = mapearErroresBackend(error);
    expect(errores.fechaVencimiento).toBe("La certificación está vencida");
    expect(errores.fechaNacimiento).toBe("La fecha de nacimiento debe ser pasada");
  });

  it("mapea PERSONA_DUPLICADA a dni y email", () => {
    const error = {
      response: {
        data: {
          codigo: "PERSONA_DUPLICADA",
          mensaje: "Ya existe una persona con ese DNI o correo",
        },
      },
    };
    const errores = mapearErroresBackend(error);
    expect(errores.dni).toBeDefined();
    expect(errores.email).toBeDefined();
  });

  it("mapea CERTIFICACION_DUPLICADA a numeroCertificacion", () => {
    const error = {
      response: {
        data: {
          codigo: "CERTIFICACION_DUPLICADA",
          mensaje: "Ya existe una certificación con ese número",
        },
      },
    };
    const errores = mapearErroresBackend(error);
    expect(errores.numeroCertificacion).toBe("Ya existe una certificación con ese número");
  });

  it("mapea CERTIFICACION_VENCIDA a fechaVencimiento", () => {
    const error = {
      response: {
        data: {
          codigo: "CERTIFICACION_VENCIDA",
          mensaje: "La certificación está vencida",
        },
      },
    };
    const errores = mapearErroresBackend(error);
    expect(errores.fechaVencimiento).toBe("La certificación está vencida");
  });
});
