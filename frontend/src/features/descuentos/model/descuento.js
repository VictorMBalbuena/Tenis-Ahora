/**
 * Modelo del descuento, alineado con la entidad real del backend:
 *
 *   Descuento { id, codigo, tipo, porcentaje, montoFijo, condiciones, activo }
 *
 * Dos cosas de la entidad que condicionan toda la pantalla:
 *
 * 1. No hay campo `nombre`, pero sí `codigo`: es lo que el socio escribe al
 *    reservar para que se le aplique el descuento, y es único. Las
 *    condiciones son el texto libre que lo explica.
 *
 * 2. `porcentaje` y `montoFijo` son los dos anulables, así que el modelo no
 *    impide que vengan los dos, o ninguno. Son formas excluyentes de calcular
 *    el mismo beneficio, así que la pantalla obliga a elegir una.
 *
 * ---------------------------------------------------------------------------
 * PENDIENTE DE DEFINICIÓN CON EL EQUIPO
 *
 * El mockup de DIS 05 pide bastante más de lo que la entidad modela:
 * nombre, código, descripción, a qué se aplica (reservas, inscripciones,
 * clases), tope máximo por operación, vigencia desde y hasta, cantidad
 * mínima de horas, acumulable con otros, y tres interruptores de
 * comportamiento.
 *
 * El más importante es la VIGENCIA: sin fecha desde y hasta no se puede
 * saber si un descuento está vigente, y la tarjeta "Por vencer" del mockup
 * no tiene de dónde salir. Hoy lo único que hay es el booleano `activo`.
 *
 * Al revés también pasa: la entidad tiene `montoFijo`, que el mockup nunca
 * contempló. Se incluyó en la pantalla porque existe en el modelo.
 * ---------------------------------------------------------------------------
 */

export const TipoDescuento = {
  PAQUETE_HORAS: "PAQUETE_HORAS",
  LIGA_REGULAR: "LIGA_REGULAR",
  ESCUELA_AFILIADA: "ESCUELA_AFILIADA",
  PROMOCIONAL: "PROMOCIONAL",
};

export const ETIQUETA_TIPO = {
  [TipoDescuento.PAQUETE_HORAS]: "Paquete de horas",
  [TipoDescuento.LIGA_REGULAR]: "Liga regular",
  [TipoDescuento.ESCUELA_AFILIADA]: "Escuela afiliada",
  [TipoDescuento.PROMOCIONAL]: "Promocional",
};

/** Largo máximo del código, igual que la columna del backend. */
export const LARGO_CODIGO = 30;

/**
 * Deja el código como se guarda: mayúsculas, sin espacios, solo letras y números.
 *
 * Se normaliza mientras se escribe para que lo que se ve en el formulario sea
 * exactamente lo que después hay que escribir al reservar.
 */
export function normalizarCodigo(valor) {
  return String(valor ?? "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "")
    .slice(0, LARGO_CODIGO);
}

/** Si ese código sirve, y si no, por qué. */
export function validarCodigo(valor) {
  const codigo = normalizarCodigo(valor);
  if (!codigo) return "El código es obligatorio.";
  return "";
}

/** Cómo se calcula el beneficio. No es un campo: se deduce de cuál vino. */
export const FormaDeCalculo = {
  PORCENTAJE: "PORCENTAJE",
  MONTO_FIJO: "MONTO_FIJO",
};

export const ETIQUETA_CALCULO = {
  [FormaDeCalculo.PORCENTAJE]: "Porcentaje",
  [FormaDeCalculo.MONTO_FIJO]: "Monto fijo",
};

/** Los dos campos son anulables, así que puede no haber ninguno cargado. */
export function formaDeCalculo(descuento) {
  if (descuento.porcentaje != null) return FormaDeCalculo.PORCENTAJE;
  if (descuento.montoFijo != null) return FormaDeCalculo.MONTO_FIJO;
  return null;
}

/** Pesos argentinos, sin decimales cuando son redondos. */
export function pesos(monto) {
  if (monto == null) return "—";
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 2,
  }).format(Number(monto));
}

/** El beneficio escrito como se lee en la fila: "15 %" o "$ 20.000". */
export function beneficio(descuento) {
  const forma = formaDeCalculo(descuento);
  if (forma === FormaDeCalculo.PORCENTAJE) {
    return `${Number(descuento.porcentaje)} %`;
  }
  if (forma === FormaDeCalculo.MONTO_FIJO) {
    return pesos(descuento.montoFijo);
  }
  return "Sin definir";
}

/**
 * Cuánto se bonifica sobre un monto dado.
 *
 * Sirve para el criterio de aceptación del ticket: la pantalla tiene que
 * mostrar el monto original, el descuento y el total.
 */
export function calcularSobre(descuento, monto) {
  const base = Number(monto) || 0;
  const forma = formaDeCalculo(descuento);

  if (forma === FormaDeCalculo.PORCENTAJE) {
    return Math.round(base * (Number(descuento.porcentaje) / 100) * 100) / 100;
  }
  if (forma === FormaDeCalculo.MONTO_FIJO) {
    // Nunca puede bonificar más que el total: no existe un cobro negativo.
    return Math.min(Number(descuento.montoFijo), base);
  }
  return 0;
}

/** Etiqueta visible del descuento. La entidad no tiene nombre propio. */
export function etiqueta(descuento) {
  return descuento.codigo || etiquetaTipo(descuento);
}

/** A quién alcanza el beneficio. Es otra cosa que el código: dos códigos pueden ser del mismo tipo. */
export function etiquetaTipo(descuento) {
  return ETIQUETA_TIPO[descuento.tipo] ?? descuento.tipo;
}

/** Resumen que alimenta las tarjetas de arriba del listado. */
export function resumirDescuentos(descuentos, totalElementos = descuentos.length) {
  return {
    total: totalElementos,
    activos: descuentos.filter((d) => d.activo).length,
    inactivos: descuentos.filter((d) => !d.activo).length,
    sinBeneficio: descuentos.filter((d) => formaDeCalculo(d) === null).length,
  };
}
