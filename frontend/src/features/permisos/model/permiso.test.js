import { describe, expect, it } from "vitest";
import {
  rolesEditables,
  alternarEspecial,
  alternarModuloEntero,
  alternarPermiso,
  contarPermisos,
  etiquetaDeRolBase,
  mismosPermisos,
  tieneEspecial,
  tienePermiso,
  validarCambioDeEstado,
  validarRol,
} from "./permiso";

const ACCIONES = ["VER", "CREAR", "EDITAR", "ELIMINAR"];

const matriz = {
  modulos: { COBROS: ["VER", "CREAR"], RESERVAS: ["VER"] },
  especiales: ["ANULAR_COBROS"],
};

describe("lectura de la matriz (CU75)", () => {
  it("dice si un rol tiene marcada una acción en un módulo", () => {
    expect(tienePermiso(matriz, "COBROS", "VER")).toBe(true);
    expect(tienePermiso(matriz, "COBROS", "ELIMINAR")).toBe(false);
    expect(tienePermiso(matriz, "REPORTES", "VER")).toBe(false);
  });

  it("dice si tiene un permiso especial", () => {
    expect(tieneEspecial(matriz, "ANULAR_COBROS")).toBe(true);
    expect(tieneEspecial(matriz, "BLOQUEAR_CANCHAS")).toBe(false);
  });

  it("una matriz vacía o nula no rompe nada", () => {
    expect(tienePermiso(null, "COBROS", "VER")).toBe(false);
    expect(tieneEspecial(undefined, "ANULAR_COBROS")).toBe(false);
    expect(contarPermisos(null)).toBe(0);
  });

  it("cuenta todas las casillas marcadas, incluidos los especiales", () => {
    expect(contarPermisos(matriz)).toBe(4);
  });
});

describe("edición de la matriz (CU75)", () => {
  it("marcar una casilla devuelve una matriz nueva y no toca la original", () => {
    const siguiente = alternarPermiso(matriz, "COBROS", "EDITAR");

    expect(tienePermiso(siguiente, "COBROS", "EDITAR")).toBe(true);
    // La original queda intacta: React necesita que cambie la identidad del objeto
    expect(tienePermiso(matriz, "COBROS", "EDITAR")).toBe(false);
    expect(siguiente).not.toBe(matriz);
  });

  it("desmarcar la última acción saca el módulo entero", () => {
    const siguiente = alternarPermiso(matriz, "RESERVAS", "VER");

    expect(siguiente.modulos.RESERVAS).toBeUndefined();
  });

  it("marcar un especial no pierde los módulos", () => {
    const siguiente = alternarEspecial(matriz, "BLOQUEAR_CANCHAS");

    expect(siguiente.especiales).toContain("BLOQUEAR_CANCHAS");
    expect(siguiente.especiales).toContain("ANULAR_COBROS");
    expect(siguiente.modulos).toEqual(matriz.modulos);
  });

  it("la casilla 'Todo' marca las cuatro acciones de una vez", () => {
    const siguiente = alternarModuloEntero(matriz, "RESERVAS", ACCIONES);

    expect(siguiente.modulos.RESERVAS).toEqual(ACCIONES);
  });

  it("la casilla 'Todo' sobre un módulo completo lo desmarca entero", () => {
    const completo = { modulos: { RESERVAS: ACCIONES }, especiales: [] };

    const siguiente = alternarModuloEntero(completo, "RESERVAS", ACCIONES);

    expect(siguiente.modulos.RESERVAS).toBeUndefined();
  });
});

describe("comparación de matrices", () => {
  it("dos matrices con lo mismo en distinto orden son iguales", () => {
    const otra = {
      modulos: { RESERVAS: ["VER"], COBROS: ["CREAR", "VER"] },
      especiales: ["ANULAR_COBROS"],
    };

    expect(mismosPermisos(matriz, otra)).toBe(true);
  });

  it("una casilla de diferencia alcanza para que no sean iguales", () => {
    expect(mismosPermisos(matriz, alternarPermiso(matriz, "COBROS", "EDITAR"))).toBe(false);
  });
});

describe("alta de un rol (CU85)", () => {
  it("exige nombre y rol base", () => {
    const { valido, errores } = validarRol({ nombre: "", rolBase: null });

    expect(valido).toBe(false);
    expect(errores.nombre).toBeTruthy();
    expect(errores.rolBase).toBeTruthy();
  });

  it("rechaza un nombre de una sola letra", () => {
    expect(validarRol({ nombre: "R", rolBase: "EMPLEADO" }).valido).toBe(false);
  });

  it("acepta un rol bien cargado", () => {
    expect(validarRol({ nombre: "Recepción", rolBase: "EMPLEADO" }).valido).toBe(true);
  });
});

describe("baja de una cuenta (CU76)", () => {
  it("deshabilitar sin motivo no se puede: lo exige RF76", () => {
    const { valido, errores } = validarCambioDeEstado({ activa: false, motivo: "   " });

    expect(valido).toBe(false);
    expect(errores.motivo).toBeTruthy();
  });

  it("deshabilitar con motivo sí", () => {
    expect(validarCambioDeEstado({ activa: false, motivo: "Dejó el club" }).valido).toBe(true);
  });

  it("volver a habilitar no pide motivo", () => {
    expect(validarCambioDeEstado({ activa: true, motivo: "" }).valido).toBe(true);
  });
});

describe("etiquetas", () => {
  it("traduce el rol base", () => {
    expect(etiquetaDeRolBase("ADMINISTRADOR")).toBe("Administrador");
  });

  it("un rol que no conoce lo muestra tal cual, sin romper", () => {
    expect(etiquetaDeRolBase("RARO")).toBe("RARO");
    expect(etiquetaDeRolBase(null)).toBe("—");
  });
});

describe("rolesEditables", () => {
  it("saca de la lista al rol Administrador base y deja el resto", () => {
    const roles = [
      { id: 1, nombre: "Administrador", base: true, rolBase: "ADMINISTRADOR" },
      { id: 2, nombre: "Empleado", base: true, rolBase: "EMPLEADO" },
      { id: 7, nombre: "Coordinador", base: false, rolBase: "ADMINISTRADOR" },
    ];

    expect(rolesEditables(roles).map((rol) => rol.id)).toEqual([2, 7]);
  });
});
