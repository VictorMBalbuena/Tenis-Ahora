// Lectura del formato único de error de la API (DIS 06): { codigo, mensaje, campos, fecha, ruta }. Genérico para toda la app.

export function codigoDeError(error) {
  return error?.response?.data?.codigo ?? null;
}

export function mensajeDeError(error, alternativa = "No se pudo completar la operación.") {
  if (!error?.response) {
    return "No se pudo conectar con el servidor. Revisá que el backend esté levantado.";
  }
  if (error.response.status === 403) {
    return "No tenés permisos para hacer esto.";
  }
  return error.response.data?.mensaje || alternativa;
}

export function camposDeError(error) {
  return error?.response?.data?.campos ?? null;
}
