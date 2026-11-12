import { describe, expect, it } from "vitest";
import {
  CAMPOS_EDITABLES,
  LARGO_MAXIMO_PASSWORD,
  PREFERENCIAS_POR_DEFECTO,
  TAMANIO_MAXIMO_FOTO,
  aDatosDeFormulario,
  conPreferenciasPorDefecto,
  fortalezaPassword,
  iniciales,
  soloLoQueCambio,
  validarCambioPassword,
  validarFoto,
  validarPerfil,
} from "../perfil";

const perfil = {
  id: 12,
  nombre: "Ana",
  apellido: "Gómez",
  dni: "30123456",
  fechaNacimiento: "1990-05-17",
  calle: "Av. Siempreviva",
  numeroCalle: "742",
  codigoArea: "11",
  numeroTelefono: "22786604",
  email: "ana@correo.com",
  fotoUrl: null,
  rol: "SOCIO",
};

describe("iniciales", () => {
  it("toma la del nombre y la del apellido", () => {
    expect(iniciales(perfil)).toBe("AG");
  });

  it("si no hay nombre, cae a la del correo", () => {
    expect(iniciales({ email: "zoe@correo.com" })).toBe("Z");
  });

  it("sin nada muestra un signo de pregunta", () => {
    expect(iniciales({})).toBe("?");
  });
});

describe("aDatosDeFormulario", () => {
  it("deja los nulos como cadena vacía, que es lo que espera un input", () => {
    const datos = aDatosDeFormulario({ ...perfil, fechaNacimiento: null, calle: null });
    expect(datos.fechaNacimiento).toBe("");
    expect(datos.calle).toBe("");
  });
});

describe("soloLoQueCambio", () => {
  it("manda solo los campos tocados", () => {
    const original = aDatosDeFormulario(perfil);
    const cambios = soloLoQueCambio({ ...original, numeroTelefono: "44556677" }, original);
    expect(cambios).toEqual({ numeroTelefono: "44556677" });
  });

  it("sin cambios devuelve un objeto vacío", () => {
    const original = aDatosDeFormulario(perfil);
    expect(soloLoQueCambio({ ...original }, original)).toEqual({});
  });

  it("nunca manda el DNI, aunque venga distinto", () => {
    const original = aDatosDeFormulario(perfil);
    const cambios = soloLoQueCambio({ ...original, dni: "99999999" }, original);
    expect(cambios.dni).toBeUndefined();
    expect(CAMPOS_EDITABLES).not.toContain("dni");
  });
});

describe("validarPerfil", () => {
  it("sin errores con datos válidos", () => {
    expect(validarPerfil(aDatosDeFormulario(perfil))).toEqual({});
  });

  it("exige nombre, apellido y correo", () => {
    const errores = validarPerfil({ ...aDatosDeFormulario(perfil), nombre: "", apellido: "", email: "" });
    expect(errores.nombre).toBeDefined();
    expect(errores.apellido).toBeDefined();
    expect(errores.email).toBeDefined();
  });

  it("rechaza un correo con formato inválido", () => {
    expect(validarPerfil({ ...aDatosDeFormulario(perfil), email: "no-es-correo" }).email).toBeDefined();
  });

  it("rechaza una fecha de nacimiento futura", () => {
    expect(validarPerfil({ ...aDatosDeFormulario(perfil), fechaNacimiento: "2099-01-01" }).fechaNacimiento).toBeDefined();
  });

  it("usa la misma regla de teléfono que Usuarios", () => {
    expect(validarPerfil({ ...aDatosDeFormulario(perfil), numeroTelefono: "123" }).numeroTelefono).toBeDefined();
  });
});

describe("validarCambioPassword", () => {
  const validos = { passwordActual: "ClaveVieja1", passwordNueva: "ClaveNueva1", repetir: "ClaveNueva1" };

  it("sin errores con datos válidos", () => {
    expect(validarCambioPassword(validos)).toEqual({});
  });

  it("exige la contraseña actual", () => {
    expect(validarCambioPassword({ ...validos, passwordActual: "" }).passwordActual).toBeDefined();
  });

  it("rechaza una contraseña nueva demasiado corta", () => {
    expect(validarCambioPassword({ ...validos, passwordNueva: "corta", repetir: "corta" }).passwordNueva).toBeDefined();
  });

  it("rechaza una contraseña nueva más larga que el máximo", () => {
    const larga = "a".repeat(LARGO_MAXIMO_PASSWORD + 1);
    expect(validarCambioPassword({ ...validos, passwordNueva: larga, repetir: larga }).passwordNueva).toBeDefined();
  });

  it("no deja poner la misma que ya tenía", () => {
    expect(
      validarCambioPassword({ passwordActual: "ClaveVieja1", passwordNueva: "ClaveVieja1", repetir: "ClaveVieja1" })
        .passwordNueva,
    ).toBe("Tiene que ser distinta de la actual.");
  });

  it("avisa cuando las dos nuevas no coinciden", () => {
    expect(validarCambioPassword({ ...validos, repetir: "otra-cosa-1" }).repetir).toBeDefined();
  });
});

describe("fortalezaPassword", () => {
  it("sin contraseña no dice nada", () => {
    expect(fortalezaPassword("")).toEqual({ nivel: 0, etiqueta: "" });
  });

  it("marca como débil una que solo cumple el largo", () => {
    expect(fortalezaPassword("12345678").etiqueta).toBe("Débil");
  });

  it("marca como fuerte una larga con mayúsculas, números y símbolos", () => {
    expect(fortalezaPassword("Clave-Muy-Larga-99").etiqueta).toBe("Fuerte");
  });

  it("nunca pasa del nivel 3", () => {
    expect(fortalezaPassword("Clave-Muy-Larga-999!!").nivel).toBeLessThanOrEqual(3);
  });
});

describe("validarFoto", () => {
  const archivo = (type, size) => ({ type, size });

  it("acepta JPG, PNG y WebP dentro del tamaño", () => {
    expect(validarFoto(archivo("image/jpeg", 500_000))).toBe("");
    expect(validarFoto(archivo("image/png", 500_000))).toBe("");
    expect(validarFoto(archivo("image/webp", 500_000))).toBe("");
  });

  it("rechaza otros formatos", () => {
    expect(validarFoto(archivo("application/pdf", 1000))).toContain("JPG, PNG o WebP");
  });

  it("rechaza una foto más grande que el máximo", () => {
    expect(validarFoto(archivo("image/png", TAMANIO_MAXIMO_FOTO + 1))).toContain("2 MB");
  });

  it("sin archivo pide elegir uno", () => {
    expect(validarFoto(null)).toBe("Elegí una imagen.");
  });
});

describe("conPreferenciasPorDefecto", () => {
  it("completa lo que el backend no mandó", () => {
    expect(conPreferenciasPorDefecto({ novedadesDelClub: true })).toEqual({
      ...PREFERENCIAS_POR_DEFECTO,
      novedadesDelClub: true,
    });
  });

  it("sin respuesta usa todo por defecto", () => {
    expect(conPreferenciasPorDefecto(null)).toEqual(PREFERENCIAS_POR_DEFECTO);
  });
});
