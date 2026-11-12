/**
 * El documento que se manda a la impresora.
 *
 * Es un HTML completo y autosuficiente: trae sus propios estilos y no depende
 * de nada de la aplicación. Se imprime dentro de un iframe, así que lo que sale
 * en el papel es exactamente esto y nada más.
 *
 * El intento anterior era al revés: montaba el listado en la página y escondía
 * el resto con `@media print`. Eso ataba el papel al ciclo de vida de React y a
 * CSS global, y con StrictMode (que ejecuta los efectos dos veces) llegaba a
 * imprimir con todo escondido y el listado ya desmontado: hojas en blanco.
 *
 * Esta función es pura: recibe datos y devuelve texto. Por eso se puede probar
 * sin navegador y sin impresora.
 */

/** Todo lo que se mete en el HTML viene de la base: hay que escaparlo. */
export function escaparHtml(valor) {
  return String(valor ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/** La fecha y la hora de emisión, como se leen en Argentina. */
export function emitidoEl(ahora = new Date()) {
  const dos = (n) => String(n).padStart(2, "0");
  return `${dos(ahora.getDate())}/${dos(ahora.getMonth() + 1)}/${ahora.getFullYear()} ${dos(ahora.getHours())}:${dos(ahora.getMinutes())}`;
}

/**
 * Los estilos del papel.
 *
 * Todo en puntos y milímetros: son unidades de impresión, no de pantalla. Van
 * en negro sobre blanco a propósito, sin importar si la aplicación está en modo
 * oscuro, porque el papel siempre es blanco.
 */
const ESTILOS = `
  @page { margin: 18mm; }

  * { box-sizing: border-box; }

  body {
    margin: 0;
    color: #111111;
    background: #ffffff;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  }

  .cabecera {
    border-bottom: 1.5pt solid #1f4f3f;
    padding-bottom: 8pt;
    margin-bottom: 12pt;
  }

  .club {
    font-size: 8pt;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: #1f4f3f;
    margin: 0;
  }

  h1 { font-size: 16pt; margin: 3pt 0 0; }

  .bajada { font-size: 9pt; color: #4b5157; margin: 2pt 0 0; }

  .emision { font-size: 8pt; color: #4b5157; margin: 4pt 0 0; }

  .aviso {
    font-size: 8.5pt;
    margin: 6pt 0 0;
    padding: 4pt 6pt;
    border: 0.75pt solid #a1560a;
    color: #a1560a;
  }

  table { width: 100%; border-collapse: collapse; font-size: 8.5pt; }

  th, td {
    border-bottom: 0.5pt solid #c9cdca;
    padding: 4pt 5pt;
    text-align: left;
    vertical-align: top;
  }

  th {
    font-size: 7.5pt;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    border-bottom: 1pt solid #111111;
  }

  /* La cabecera de la tabla se repite arriba de cada hoja. */
  thead { display: table-header-group; }

  tr { break-inside: avoid; }

  .a-la-derecha { text-align: right; }

  .vacio, .pie { font-size: 8.5pt; color: #4b5157; margin-top: 10pt; }
`;

function celdasDe(fila, columnas, etiqueta) {
  return fila
    .map((celda, i) => {
      const clase = columnas[i]?.alineacion === "derecha" ? ' class="a-la-derecha"' : "";
      return `<${etiqueta}${clase}>${escaparHtml(celda)}</${etiqueta}>`;
    })
    .join("");
}

/**
 * El HTML completo de un listado, listo para imprimir.
 *
 * @param {object} opciones
 * @param {object} opciones.listado   definición del listado (título, columnas, aviso)
 * @param {Array<Array<string>>} opciones.filas
 * @param {string} [opciones.emitidoPor]  quién lo pidió, para que conste en el papel
 * @param {number} [opciones.tope]    si se llegó a este número, el listado salió cortado
 * @param {Date}   [opciones.ahora]   inyectable para poder probar la fecha
 */
export function documentoDeListado({ listado, filas = [], emitidoPor = "", tope = Infinity, ahora = new Date() }) {
  const columnas = listado.columnas ?? [];

  const cabecera = columnas
    .map((c) => `<th${c.alineacion === "derecha" ? ' class="a-la-derecha"' : ""}>${escaparHtml(c.titulo)}</th>`)
    .join("");

  const cuerpo = filas.map((fila) => `<tr>${celdasDe(fila, columnas, "td")}</tr>`).join("");

  const tabla =
    filas.length === 0
      ? '<p class="vacio">No hay datos para este listado.</p>'
      : `<table><thead><tr>${cabecera}</tr></thead><tbody>${cuerpo}</tbody></table>`;

  const cortado = filas.length >= tope ? ` — el listado se corta en las primeras ${tope}.` : "";
  const porQuien = emitidoPor ? ` por ${escaparHtml(emitidoPor)}` : "";

  return `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>${escaparHtml(listado.titulo)}</title>
<style>${ESTILOS}</style>
</head>
<body>
<header class="cabecera">
  <p class="club">Tenis Ahora — Club &amp; Sistema Deportivo</p>
  <h1>${escaparHtml(listado.titulo)}</h1>
  ${listado.descripcion ? `<p class="bajada">${escaparHtml(listado.descripcion)}</p>` : ""}
  <p class="emision">Emitido el ${emitidoEl(ahora)}${porQuien}</p>
  ${listado.aviso ? `<p class="aviso">${escaparHtml(listado.aviso)}</p>` : ""}
</header>
${tabla}
<footer class="pie">${filas.length} ${filas.length === 1 ? "fila" : "filas"}${cortado}</footer>
</body>
</html>`;
}
