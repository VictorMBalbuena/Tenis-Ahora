import { describe, expect, it } from "vitest";
import {
  dniFormateado,
  domicilioFormateado,
  fechaCorta,
  nombreCompleto,
  resumirUsuarios,
  telefonoFormateado,
  validarPersona,
} from "./usuario";

describe("modelo de usuario y validaciones", () => {
  describe("validarPersona en modo Alta", () => {
    it("detecta campos requeridos faltantes", () => {
      const res = validarPersona({}, false);

      expect(res.valido).toBe(false);
      expect(res.errores.nombre).toBe("El nombre es obligatorio.");
      expect(res.errores.apellido).toBe("El apellido es obligatorio.");
      expect(res.errores.dni).toBe("El DNI es obligatorio.");
      expect(res.errores.fechaNacimiento).toBe("La fecha de nacimiento es obligatoria.");
      expect(res.errores.calle).toBe("La calle es obligatoria.");
      expect(res.errores.numeroCalle).toBe("El número de calle es obligatorio.");
      expect(res.errores.codigoArea).toBe("El código de área es obligatorio.");
      expect(res.errores.numeroTelefono).toBe("El número de teléfono es obligatorio.");
      expect(res.errores.email).toBe("El correo electrónico es obligatorio.");
      expect(res.errores.password).toBe("La contraseña es obligatoria.");
    });

    it("valida formatos numéricos y regex según el negocio", () => {
      const datosInvalidos = {
        nombre: "Ana",
        apellido: "Gómez",
        dni: "123", // Muy corto
        fechaNacimiento: "2099-01-01", // Fecha futura
        calle: "Av. Siempreviva",
        numeroCalle: "abc", // No numérico
        codigoArea: "011", // Con 0 inicial
        numeroTelefono: "1512345678", // Con 15 o muy largo
        email: "correo-invalido",
        password: "123", // Menos de 8 caracteres
      };

      const res = validarPersona(datosInvalidos, false);

      expect(res.valido).toBe(false);
      expect(res.errores.dni).toBe("El DNI debe tener 7 u 8 dígitos numéricos.");
      expect(res.errores.fechaNacimiento).toBe("La fecha de nacimiento debe ser pasada.");
      expect(res.errores.numeroCalle).toBe("Solo se permiten números.");
      expect(res.errores.codigoArea).toBe("Sin el 0 (ej: 11, 351).");
      expect(res.errores.numeroTelefono).toBe("Sin el 15, entre 6 y 8 dígitos.");
      expect(res.errores.email).toBe("El correo electrónico no tiene un formato válido.");
      expect(res.errores.password).toBe("La contraseña debe tener entre 8 y 72 caracteres.");
    });

    it("rechaza nombres y apellidos que contengan números o caracteres no permitidos", () => {
      const conNumeros = {
        nombre: "Admin 101",
        apellido: "Pérez 2",
        dni: "30123456",
        fechaNacimiento: "1990-05-17",
        calle: "Av. Siempreviva",
        numeroCalle: "742",
        codigoArea: "11",
        numeroTelefono: "12345678",
        email: "ana@correo.com",
        password: "PasswordSeguro123!",
      };

      const res = validarPersona(conNumeros, false);
      expect(res.valido).toBe(false);
      expect(res.errores.nombre).toBe("El nombre solo puede contener letras y espacios.");
      expect(res.errores.apellido).toBe("El apellido solo puede contener letras y espacios.");
    });

    it("acepta un objeto con todos los datos válidos", () => {
      const datosValidos = {
        nombre: "Ana",
        apellido: "Gómez",
        dni: "30123456",
        fechaNacimiento: "1990-05-17",
        calle: "Av. Siempreviva",
        numeroCalle: "742",
        codigoArea: "11",
        numeroTelefono: "12345678",
        email: "ana@correo.com",
        password: "PasswordSeguro123!",
      };

      const res = validarPersona(datosValidos, false);

      expect(res.valido).toBe(true);
      expect(res.errores).toEqual({});
    });
  });

  describe("validarPersona en modo Edición", () => {
    it("no exige contraseña en edición", () => {
      const datosEdicion = {
        nombre: "Ana",
        apellido: "Gómez",
        dni: "30123456",
        calle: "Nueva Calle",
        numeroCalle: "123",
        codigoArea: "11",
        numeroTelefono: "12345678",
        email: "ana@correo.com",
      };

      const res = validarPersona(datosEdicion, true);

      expect(res.valido).toBe(true);
      expect(res.errores.password).toBeUndefined();
    });
  });

  describe("helpers de formato", () => {
    it("dniFormateado formatea con puntos", () => {
      expect(dniFormateado("30123456")).toBe("30.123.456");
      expect(dniFormateado("8123456")).toBe("8.123.456");
      expect(dniFormateado("")).toBe("—");
    });

    it("fechaCorta convierte YYYY-MM-DD a DD/MM/YYYY", () => {
      expect(fechaCorta("1990-05-17")).toBe("17/05/1990");
      expect(fechaCorta("")).toBe("—");
    });

    it("nombreCompleto une nombre y apellido", () => {
      expect(nombreCompleto({ nombre: "Carlos", apellido: "López" })).toBe("Carlos López");
      expect(nombreCompleto(null)).toBe("");
    });

    it("telefonoFormateado y domicilioFormateado", () => {
      expect(telefonoFormateado("11", "12345678")).toBe("(11) 12345678");
      expect(domicilioFormateado("San Martín", "123")).toBe("San Martín 123");
    });

    it("resumirUsuarios calcula totales, activos e inactivos", () => {
      const usuarios = [
        { id: 1, activo: true },
        { id: 2, activo: false },
        { id: 3, activo: true },
      ];

      expect(resumirUsuarios(usuarios)).toEqual({
        total: 3,
        activos: 2,
        inactivos: 1,
      });
    });
  });
});
