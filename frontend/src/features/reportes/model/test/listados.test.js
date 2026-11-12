import { describe, expect, it } from "vitest";
import { LISTADOS, listadoPorId } from "../listados";

/** Las columnas y las celdas de una fila tienen que coincidir en cantidad, o el impreso sale corrido. */
function filaDe(id, datos) {
  const listado = listadoPorId(id);
  const filas = listado.mapear(datos);
  expect(filas).toHaveLength(datos.length);
  filas.forEach((fila) => expect(fila).toHaveLength(listado.columnas.length));
  return filas[0];
}

describe("catalogo de listados", () => {
  it("son los diez que pide la pantalla", () => {
    expect(LISTADOS).toHaveLength(10);
  });

  it("cada uno tiene id unico, titulo, columnas y forma de armar las filas", () => {
    const ids = LISTADOS.map((l) => l.id);
    expect(new Set(ids).size).toBe(ids.length);

    LISTADOS.forEach((listado) => {
      expect(listado.titulo).toBeTruthy();
      expect(listado.descripcion).toBeTruthy();
      expect(listado.columnas.length).toBeGreaterThan(0);
      expect(typeof listado.mapear).toBe("function");
    });
  });

  it("sin datos no hay filas, pero el listado sigue existiendo", () => {
    LISTADOS.forEach((listado) => {
      expect(listado.mapear([])).toEqual([]);
    });
  });

  it("un id que no existe no devuelve nada", () => {
    expect(listadoPorId("inventado")).toBeUndefined();
  });
});

describe("datos de usuarios", () => {
  it("pone apellido y nombre juntos, y dice si esta de baja", () => {
    const fila = filaDe("usuarios", [
      { nombre: "Ana", apellido: "Pérez", dni: "30111222", email: "ana@club.com", rol: "SOCIO", activo: true },
    ]);

    expect(fila[0]).toBe("Pérez, Ana");
    expect(fila[3]).toBe("Socio");
    expect(fila[4]).toBe("Activo");
  });

  it("una persona dada de baja se imprime igual, pero marcada", () => {
    const fila = filaDe("usuarios", [{ nombre: "Ana", apellido: "Pérez", activo: false }]);

    expect(fila[4]).toBe("Dado de baja");
  });

  it("un campo que falta sale con raya y no rompe la fila", () => {
    const fila = filaDe("usuarios", [{ nombre: "Ana", apellido: "Pérez" }]);

    expect(fila[1]).toBe("—");
    expect(fila[2]).toBe("—");
  });
});

describe("tipos de cancha", () => {
  it("una cancha en mantenimiento no figura como disponible", () => {
    const fila = filaDe("canchas", [
      { identificador: "Cancha 1", tipoSuperficie: "POLVO_DE_LADRILLO", capacidad: 4, precioPorHora: 5000, activa: true, enMantenimiento: true },
    ]);

    expect(fila[4]).toBe("En mantenimiento");
  });

  it("una cancha deshabilitada tampoco", () => {
    const fila = filaDe("canchas", [{ identificador: "Cancha 2", activa: false, enMantenimiento: false }]);

    expect(fila[4]).toBe("Deshabilitada");
  });

  it("una cancha operativa se imprime como disponible, con su precio en pesos", () => {
    const fila = filaDe("canchas", [{ identificador: "Cancha 3", activa: true, enMantenimiento: false, precioPorHora: 5000 }]);

    expect(fila[4]).toBe("Disponible");
    expect(fila[3]).toContain("5.000");
  });
});

describe("clases programadas", () => {
  const clase = {
    codigo: "CL-001",
    nombre: "Iniciación adultos",
    nivel: "PRINCIPIANTE",
    profesor: "Juan Gómez",
    cancha: "Cancha 1",
    dias: ["WEDNESDAY", "MONDAY"],
    horaInicio: "18:00:00",
    horaFin: "19:30:00",
    fechaInicio: "2026-03-02",
    fechaFin: "2026-07-03",
    cupoOcupado: 6,
    cupoMaximo: 10,
    estado: "PROGRAMADA",
  };

  it("imprime los dias ordenados de lunes a domingo, no como vinieron", () => {
    const fila = filaDe("clases", [clase]);

    expect(fila[5]).toBe("Lu, Mi");
  });

  it("el horario y la vigencia salen legibles, sin segundos ni formato ISO", () => {
    const fila = filaDe("clases", [clase]);

    expect(fila[6]).toBe("18:00 a 19:30");
    expect(fila[7]).toBe("02/03/2026 a 03/07/2026");
  });

  it("el cupo se lee como ocupados sobre el maximo", () => {
    const fila = filaDe("clases", [clase]);

    expect(fila[8]).toBe("6 / 10");
  });
});

describe("resultados de partidos", () => {
  it("en una liga las vueltas son fechas y en un torneo, rondas", () => {
    const [deLiga, deTorneo] = listadoPorId("partidos").mapear([
      { ronda: 2, torneo: { nombre: "Liga Verano", tipo: "LIGA" }, participanteANombre: "A", participanteBNombre: "B" },
      { ronda: 2, torneo: { nombre: "Open Club", tipo: "TORNEO" }, participanteANombre: "C", participanteBNombre: "D" },
    ]);

    expect(deLiga[1]).toBe("Fecha 2");
    expect(deTorneo[1]).toBe("Ronda 2");
  });

  it("un partido sin jugar lo dice, en vez de dejar el resultado vacio", () => {
    const fila = filaDe("partidos", [
      { ronda: 1, torneo: { nombre: "Open Club" }, participanteANombre: "A", participanteBNombre: "B", sets: null, resultado: null },
    ]);

    expect(fila[2]).toBe("A vs B");
    expect(fila[5]).toBe("Sin jugar");
  });

  it("un partido jugado imprime sus sets y quien gano", () => {
    const fila = filaDe("partidos", [
      {
        ronda: 1,
        torneo: { nombre: "Open Club" },
        participanteANombre: "A",
        participanteBNombre: "B",
        sets: "6-0 6-1",
        resultado: "Gana A",
        fecha: "2026-11-10",
      },
    ]);

    expect(fila[3]).toBe("10/11/2026");
    expect(fila[4]).toBe("6-0 6-1");
    expect(fila[5]).toBe("Gana A");
  });
});

describe("cobros y recibos", () => {
  it("un cobro imprime los dos montos y su estado legible", () => {
    const fila = filaDe("cobros", [
      { codigo: "CB-1", concepto: "ALQUILER_CANCHA", medioPago: "DEBITO", cuotas: 1, montoBase: 7000, montoFinal: 6300, estado: "PAGADO" },
    ]);

    expect(fila[4]).toContain("7.000");
    expect(fila[5]).toContain("6.300");
    expect(fila[6]).toBe("Pagado");
  });

  it("un estado con guion bajo se lee como texto, no como constante", () => {
    const fila = filaDe("recibos", [{ numeroRecibo: "R-1", estado: "PENDIENTE_DE_PAGO", monto: 100 }]);

    expect(fila[5]).toBe("Pendiente de pago");
  });
});

describe("descuentos", () => {
  it("avisa en el impreso que todavia son datos de prueba", () => {
    expect(listadoPorId("descuentos").aviso).toMatch(/prueba/i);
  });

  it("ningun otro listado lleva ese aviso", () => {
    const conAviso = LISTADOS.filter((l) => l.aviso).map((l) => l.id);

    expect(conAviso).toEqual(["descuentos"]);
  });

  it("un descuento por porcentaje muestra el porcentaje y si esta vigente", () => {
    const fila = filaDe("descuentos", [
      { tipo: "LIGA_REGULAR", porcentaje: 15, montoFijo: null, condiciones: "Equipos de liga", vecesAplicado: 64, activo: true },
    ]);

    expect(fila[0]).toBe("Liga regular");
    expect(fila[1]).toBe("15 %");
    expect(fila[4]).toBe("Sí");
  });
});
