/**
 * Lectura del formato unico de error de la API (DIS 06):
 *
 *   { codigo, mensaje, campos, fecha, ruta }
 *
 * Es la misma lectura que hace la feature de insumos. Cuando esa rama se
 * mergee conviene subir una sola copia a src/lib/ y que las dos la importen.
 */

export function codigoDeError(error) {
  return error?.response?.data?.codigo ?? null;
}

export function mensajeDeError(error, alternativa = "No se pudo completar la operacion.") {
  if (!error?.response) {
    return "No se pudo conectar con el servidor. Revisa que el backend este levantado.";
  }
  if (error.response.status === 403) {
    return "No tenes permisos para hacer esto.";
  }
  return error.response.data?.mensaje || alternativa;
}

export function camposDeError(error) {
  return error?.response?.data?.campos ?? null;
}
