import { describe, expect, it } from "vitest";
import { documentoDeListado, emitidoEl, escaparHtml } from "../documento";
import { listadoPorId } from "../listados";

const listado = {
  titulo: "Datos de usuarios",
  descripcion: "Socios, empleados y administradores",
  columnas: [{ titulo: "Nombre" }, { titulo: "DNI" }, { titulo: "Monto", alineacion: "derecha" }],
};

const filas = [
  ["Pérez, Ana", "30111222", "$ 5.000"],
  ["Gómez, Juan", "28999111", "$ 7.000"],
];

/** Lo que importa del documento se puede leer parseándolo, sin impresora. */
function comoDom(html) {
  return new DOMParser().parseFromString(html, "text/html");
}

describe("documento para imprimir", () => {
  it("es un html completo y autosuficiente: no depende de la aplicacion", () => {
    const html = documentoDeListado({ listado, filas });

    expect(html.startsWith("<!doctype html>")).toBe(true);
    expect(html).toContain('<meta charset="utf-8">');
    // Los estilos van adentro: si fueran de la app, el papel saldria sin formato.
    expect(html).toContain("<style>");
    expect(html).toContain("@page");
  });

  it("lleva el titulo del listado como titulo del documento", () => {
    const doc = comoDom(documentoDeListado({ listado, filas }));

    expect(doc.title).toBe("Datos de usuarios");
    expect(doc.querySelector("h1").textContent).toBe("Datos de usuarios");
  });

  it("imprime todas las filas, con una celda por columna", () => {
    const doc = comoDom(documentoDeListado({ listado, filas }));

    const cuerpo = doc.querySelectorAll("tbody tr");
    expect(cuerpo).toHaveLength(2);
    expect([...cuerpo[0].querySelectorAll("td")].map((c) => c.textContent)).toEqual([
      "Pérez, Ana",
      "30111222",
      "$ 5.000",
    ]);
  });

  it("las columnas de numeros van alineadas a la derecha, tambien en el cuerpo", () => {
    const doc = comoDom(documentoDeListado({ listado, filas }));

    expect(doc.querySelectorAll("thead th")[2].className).toBe("a-la-derecha");
    expect(doc.querySelectorAll("tbody tr td")[2].className).toBe("a-la-derecha");
    expect(doc.querySelectorAll("tbody tr td")[0].className).toBe("");
  });

  it("dice cuantas filas tiene, en singular y en plural", () => {
    const una = comoDom(documentoDeListado({ listado, filas: [filas[0]] }));
    const dos = comoDom(documentoDeListado({ listado, filas }));

    expect(una.querySelector(".pie").textContent).toBe("1 fila");
    expect(dos.querySelector(".pie").textContent).toBe("2 filas");
  });

  it("avisa en el pie cuando el listado salio cortado por el tope", () => {
    const doc = comoDom(documentoDeListado({ listado, filas, tope: 2 }));

    expect(doc.querySelector(".pie").textContent).toContain("se corta en las primeras 2");
  });

  it("sin datos no imprime una tabla vacia: lo dice", () => {
    const doc = comoDom(documentoDeListado({ listado, filas: [] }));

    expect(doc.querySelector("table")).toBeNull();
    expect(doc.querySelector(".vacio").textContent).toBe("No hay datos para este listado.");
  });

  it("deja constancia de quien lo emitio y cuando", () => {
    const ahora = new Date(2026, 10, 6, 9, 5);
    const doc = comoDom(documentoDeListado({ listado, filas, emitidoPor: "admin@club.com", ahora }));

    expect(doc.querySelector(".emision").textContent).toBe("Emitido el 06/11/2026 09:05 por admin@club.com");
  });

  it("sin usuario no inventa quien lo emitio, pero sigue poniendo la fecha", () => {
    const ahora = new Date(2026, 0, 2, 18, 30);
    const doc = comoDom(documentoDeListado({ listado, filas, ahora }));

    expect(doc.querySelector(".emision").textContent).toBe("Emitido el 02/01/2026 18:30");
  });

  it("el aviso del listado se imprime, y si no hay no queda el recuadro", () => {
    const conAviso = comoDom(documentoDeListado({ listado: { ...listado, aviso: "Datos de prueba." }, filas }));
    const sinAviso = comoDom(documentoDeListado({ listado, filas }));

    expect(conAviso.querySelector(".aviso").textContent).toBe("Datos de prueba.");
    expect(sinAviso.querySelector(".aviso")).toBeNull();
  });
});

describe("el contenido se escapa", () => {
  it("un dato con html no se interpreta: se imprime tal cual", () => {
    const doc = comoDom(
      documentoDeListado({
        listado,
        filas: [["<script>alert(1)</script>", "&", '"comillas"']],
      }),
    );

    expect(doc.querySelectorAll("script")).toHaveLength(0);
    const celdas = [...doc.querySelectorAll("tbody td")].map((c) => c.textContent);
    expect(celdas[0]).toBe("<script>alert(1)</script>");
    expect(celdas[1]).toBe("&");
    expect(celdas[2]).toBe('"comillas"');
  });

  it("escaparHtml cubre los cinco caracteres que rompen el html", () => {
    expect(escaparHtml(`<&>"'`)).toBe("&lt;&amp;&gt;&quot;&#39;");
  });

  it("un valor que falta no imprime 'null' ni 'undefined'", () => {
    expect(escaparHtml(null)).toBe("");
    expect(escaparHtml(undefined)).toBe("");
  });
});

describe("la fecha de emision", () => {
  it("va en hora local y con dos digitos", () => {
    expect(emitidoEl(new Date(2026, 2, 9, 7, 4))).toBe("09/03/2026 07:04");
  });
});

describe("cada listado del catalogo produce un documento", () => {
  it("los diez se arman sin romperse, aun sin datos", () => {
    ["usuarios", "canchas", "profesores", "entrenadores", "clases", "entrenamientos", "partidos", "cobros", "recibos", "descuentos"].forEach(
      (id) => {
        const def = listadoPorId(id);
        const doc = comoDom(documentoDeListado({ listado: def, filas: def.mapear([]) }));

        expect(doc.querySelector("h1").textContent).toBe(def.titulo);
        expect(doc.querySelector(".vacio")).not.toBeNull();
      },
    );
  });

  it("el de descuentos imprime su aviso de datos de prueba", () => {
    const def = listadoPorId("descuentos");
    const doc = comoDom(documentoDeListado({ listado: def, filas: [] }));

    expect(doc.querySelector(".aviso").textContent).toMatch(/prueba/i);
  });
});
