// Códigos que devuelve el módulo de insumos y que la pantalla distingue; la lectura genérica vive en @/lib/erroresApi.
export { codigoDeError, mensajeDeError, camposDeError } from "@/lib/erroresApi";

export const CodigoError = {
  STOCK_INSUFICIENTE: "STOCK_INSUFICIENTE",
  INSUMO_NOMBRE_DUPLICADO: "INSUMO_NOMBRE_DUPLICADO",
};
