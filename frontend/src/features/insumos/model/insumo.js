
export const EstadoStock = {
  DISPONIBLE: "DISPONIBLE",
  STOCK_BAJO: "STOCK_BAJO",
  SIN_STOCK: "SIN_STOCK",
};

export const ETIQUETA_ESTADO = {
  [EstadoStock.DISPONIBLE]: "Disponible",
  [EstadoStock.STOCK_BAJO]: "Stock bajo",
  [EstadoStock.SIN_STOCK]: "Sin stock",
};

export const TipoMovimiento = {
  DESCUENTO: "DESCUENTO",
  REPOSICION: "REPOSICION",
};

export const ETIQUETA_MOVIMIENTO = {
  [TipoMovimiento.DESCUENTO]: "Descuento",
  [TipoMovimiento.REPOSICION]: "Reposición",
  AJUSTE: "Ajuste",
};

export function stockTotal(insumo) {
  if (insumo.stockTotal != null) return insumo.stockTotal;
  return (insumo.cantidadDisponible ?? 0) + (insumo.enUso ?? 0);
}
export function estadoDeStock(insumo) {
  if (insumo.estado) return insumo.estado;
  if (insumo.cantidadDisponible === 0) return EstadoStock.SIN_STOCK;
  if (insumo.cantidadDisponible <= insumo.cantidadMinima)
    return EstadoStock.STOCK_BAJO;
  return EstadoStock.DISPONIBLE;
}
export function sinStock(insumo) {
  return estadoDeStock(insumo) === EstadoStock.SIN_STOCK;
}

export function porcentajeDisponible(insumo) {
  const total = stockTotal(insumo);
  if (total === 0) return 0;
  return Math.round((insumo.cantidadDisponible / total) * 100);
}

export function validarInsumo({ nombre, categoriaId, cantidadDisponible, cantidadMinima }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = "Escribí el nombre del insumo.";
  if (!categoriaId) errores.categoriaId = "Elegí una categoría.";
  if (cantidadDisponible < 0) errores.cantidadDisponible = "No puede ser negativo.";
  if (cantidadMinima < 0) errores.cantidadMinima = "No puede ser negativo.";
  return errores;
}

export function fechaCorta(iso) {
  if (!iso) return "—";
  const [fecha, hora = ""] = String(iso).split("T");
  const [, mes, dia] = fecha.split("-");
  return `${dia}/${mes} ${hora.slice(0, 5)}`.trim();
}
