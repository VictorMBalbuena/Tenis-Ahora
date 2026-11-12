/**
 * Manda un documento HTML a la impresora, sin tocar la página.
 *
 * Se usa un iframe fuera de la pantalla con su propio documento adentro: lo que
 * se imprime es ese documento y nada más. No hace falta esconder la aplicación
 * con `@media print` ni depender de que React mantenga algo montado el tiempo
 * justo, que es lo que hacía salir hojas en blanco.
 *
 * El iframe se queda un rato después de `print()` a propósito: hay navegadores
 * que arman el trabajo de impresión en diferido y, si se lo saca enseguida, se
 * imprime vacío.
 */

/** Cuánto esperar antes de sacar el iframe. De sobra para que el navegador arme el trabajo. */
const ESPERA_ANTES_DE_LIMPIAR = 60_000;

export function imprimirHtml(html, { documento = document } = {}) {
  const marco = documento.createElement("iframe");
  marco.setAttribute("aria-hidden", "true");
  marco.setAttribute("title", "Documento para imprimir");
  // Fuera de la pantalla pero con tamaño de hoja: un iframe de 0x0 o invisible
  // no siempre se renderiza, y entonces no hay nada que imprimir.
  marco.style.cssText = "position:fixed;left:-10000px;top:0;width:210mm;height:297mm;border:0;";

  documento.body.appendChild(marco);

  const quitar = () => marco.remove();

  const lanzar = () => {
    try {
      marco.contentWindow.focus();
      marco.contentWindow.print();
    } finally {
      setTimeout(quitar, ESPERA_ANTES_DE_LIMPIAR);
    }
  };

  const interno = marco.contentDocument;
  if (!interno) {
    quitar();
    throw new Error("El navegador no dejó preparar el documento para imprimir.");
  }

  interno.open();
  interno.write(html);
  interno.close();

  if (interno.readyState === "complete") {
    lanzar();
  } else {
    marco.addEventListener("load", lanzar, { once: true });
  }

  return marco;
}
