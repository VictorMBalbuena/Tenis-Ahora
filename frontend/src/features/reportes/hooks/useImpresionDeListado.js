import { useCallback, useState } from "react";
import { mensajeDeError } from "@/lib/erroresApi";
import { listadoPorId } from "../model/listados";
import { documentoDeListado } from "../model/documento";
import { traerDatos, TOPE_DE_FILAS } from "../api/listadosApi";
import { imprimirHtml } from "../lib/imprimirHtml";

/**
 * Traer un listado del sistema y mandarlo a la impresora.
 *
 * Todo pasa dentro del clic: se piden los datos, se arma el documento y se
 * imprime. No queda nada en el estado de React esperando a imprimirse.
 *
 * Esto último es a propósito. Antes el documento vivía montado en la página y
 * se imprimía desde un efecto: con StrictMode, que ejecuta los efectos dos
 * veces, la segunda pasada llegaba a imprimir cuando el listado ya se había
 * desmontado, y salían hojas en blanco.
 */
export function useImpresionDeListado({ emitidoPor = "" } = {}) {
  const [trayendo, setTrayendo] = useState(null);
  const [error, setError] = useState("");

  const limpiarError = useCallback(() => setError(""), []);

  const imprimir = useCallback(
    async (id) => {
      const listado = listadoPorId(id);
      if (!listado) {
        setError(`No hay un listado definido para "${id}".`);
        return;
      }

      setTrayendo(id);
      setError("");
      try {
        const datos = await traerDatos(id);
        const filas = listado.mapear(datos ?? []);
        imprimirHtml(documentoDeListado({ listado, filas, emitidoPor, tope: TOPE_DE_FILAS }));
      } catch (fallo) {
        setError(mensajeDeError(fallo, `No se pudo armar el listado "${listado.titulo}".`));
      } finally {
        setTrayendo(null);
      }
    },
    [emitidoPor],
  );

  return { imprimir, trayendo, error, limpiarError };
}
