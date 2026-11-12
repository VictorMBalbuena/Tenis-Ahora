// Modelo del cobro, alineado con la entidad del backend: reserva o torneo, concepto, medio de pago, monto final y fecha.

/**
 * Conceptos del enum ConceptoCobro.
 *
 * Ojo con SEÑA: el enum del backend lleva eñe, así que la clave viaja con
 * eñe en el JSON. No se normaliza acá para no romper la correspondencia.
 */
export const ConceptoCobro = {
  SENA: "SEÑA",
  SALDO: "SALDO",
  INSCRIPCION: "INSCRIPCION",
  REEMBOLSO: "REEMBOLSO",
  OTRO: "OTRO",
};

export const ETIQUETA_CONCEPTO = {
  [ConceptoCobro.SENA]: "Seña",
  [ConceptoCobro.SALDO]: "Saldo",
  [ConceptoCobro.INSCRIPCION]: "Inscripción",
  [ConceptoCobro.REEMBOLSO]: "Reembolso",
  [ConceptoCobro.OTRO]: "Otro",
};

export const EstadoCobro = {
  PENDIENTE: "PENDIENTE",
  PAGADO: "PAGADO",
  VENCIDO: "VENCIDO",
  RECHAZADO: "RECHAZADO",
  ANULADO: "ANULADO",
};

export const ETIQUETA_ESTADO = {
  [EstadoCobro.PENDIENTE]: "Pendiente",
  [EstadoCobro.PAGADO]: "Pagado",
  [EstadoCobro.VENCIDO]: "Vencido",
  [EstadoCobro.RECHAZADO]: "Rechazado",
  [EstadoCobro.ANULADO]: "Anulado",
};

/** Solo se anula lo que está en curso o pagado, igual que en el backend. */
export function esAnulable(cobro) {
  return cobro.estado === EstadoCobro.PENDIENTE || cobro.estado === EstadoCobro.PAGADO;
}

export const MedioPago = {
  DEBITO: "DEBITO",
  CREDITO: "CREDITO",
  QR: "QR",
  EFECTIVO: "EFECTIVO",
};

export const ETIQUETA_MEDIO = {
  [MedioPago.DEBITO]: "Débito",
  [MedioPago.CREDITO]: "Crédito",
  [MedioPago.QR]: "Mercado Pago (QR)",
  [MedioPago.EFECTIVO]: "Efectivo",
};

export const OPCIONES_CONCEPTO = Object.values(ConceptoCobro).map((v) => ({
  value: v,
  label: ETIQUETA_CONCEPTO[v],
}));

export const OPCIONES_MEDIO = Object.values(MedioPago).map((v) => ({ value: v, label: ETIQUETA_MEDIO[v] }));

/** Un cobro apunta a una reserva o a un torneo, nunca a los dos. */
export const Origen = {
  RESERVA: "RESERVA",
  TORNEO: "TORNEO",
  INSCRIPCION: "INSCRIPCION",
};

export const ETIQUETA_ORIGEN = {
  [Origen.RESERVA]: "Alquiler de cancha",
  [Origen.TORNEO]: "Inscripción a torneo o liga",
  [Origen.INSCRIPCION]: "Inscripción",
};

export const OPCIONES_ORIGEN = Object.values(Origen).map((v) => ({ value: v, label: ETIQUETA_ORIGEN[v] }));

/** De dónde viene el cobro, deducido de cuál de las dos referencias trae. */
export function origenDe(cobro) {
  if (cobro.reserva || cobro.reservaId) return Origen.RESERVA;
  if (cobro.torneo || cobro.torneoId) return Origen.TORNEO;
  if (cobro.origen) return Origen.INSCRIPCION;
  return null;
}

/**
 * Cómo se nombra un torneo en pantalla. La entidad no tiene nombre propio:
 * se arma con la categoría y la modalidad, que es lo que lo distingue.
 */
export function etiquetaTorneo(torneo) {
  if (!torneo) return "—";
  const partes = [torneo.categoria, torneo.modalidad].filter(Boolean).map((p) => p.toLowerCase());
  const detalle = partes.length > 0 ? ` (${partes.join(", ")})` : "";
  return `Torneo #${torneo.id}${detalle}`;
}

/** Cómo se nombra una reserva: la cancha y el día, que es lo que reconoce quien cobra. */
export function etiquetaReserva(reserva) {
  if (!reserva) return "—";
  const cancha = reserva.cancha?.identificador ? ` — ${reserva.cancha.identificador}` : "";
  const dia = reserva.fecha ? ` — ${fechaCorta(reserva.fecha)}` : "";
  const hora = reserva.horaInicio ? ` ${String(reserva.horaInicio).slice(0, 5)}` : "";
  return `Reserva #${reserva.id}${cancha}${dia}${hora}`;
}

/** Texto corto de a qué se aplica el cobro, para el listado. */
export function descripcionOrigen(cobro) {
  // CobroResponse trae solo el id, no la reserva entera: por eso tambien se
  // contempla reservaId/torneoId y no unicamente el objeto anidado.
  if (cobro.origen) return cobro.origen;
  if (cobro.reserva) return etiquetaReserva(cobro.reserva);
  if (cobro.torneo) return etiquetaTorneo(cobro.torneo);
  if (cobro.reservaId) return `Reserva #${cobro.reservaId}`;
  if (cobro.torneoId) return `Torneo #${cobro.torneoId}`;
  return "—";
}

/** Pesos argentinos. */
export function pesos(monto) {
  if (monto == null || monto === "") return "—";
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 2,
  }).format(Number(monto));
}

/** Fecha con hora (2026-10-27T18:30:00) a formato local (27/10/2026 18:30). */
export function fechaHora(iso) {
  if (!iso) return "—";
  const [fecha, hora = ""] = String(iso).split("T");
  const [anio, mes, dia] = fecha.split("-");
  return `${dia}/${mes}/${anio}${hora ? ` ${hora.slice(0, 5)}` : ""}`;
}

/** Solo la fecha, sin hora. */
export function fechaCorta(iso) {
  if (!iso) return "—";
  const [anio, mes, dia] = String(iso).split("T")[0].split("-");
  return `${dia}/${mes}/${anio}`;
}

/** Un reembolso descuenta de la caja en vez de sumar (RD13). */
export function esReembolso(cobro) {
  return cobro.concepto === ConceptoCobro.REEMBOLSO;
}

/** El monto con su signo: el reembolso sale, el resto entra. */
export function montoConSigno(cobro) {
  const monto = Number(cobro.montoFinal ?? 0);
  return esReembolso(cobro) ? -monto : monto;
}

/**
 * Tipos del enum TipoDescuento. El descuento no tiene nombre propio en la
 * entidad: lo identifica su tipo, y `condiciones` es el texto libre que
 * aclara cuándo aplica.
 */
export const TipoDescuento = {
  PAQUETE_HORAS: "PAQUETE_HORAS",
  LIGA_REGULAR: "LIGA_REGULAR",
  ESCUELA_AFILIADA: "ESCUELA_AFILIADA",
};

export const ETIQUETA_TIPO_DESCUENTO = {
  [TipoDescuento.PAQUETE_HORAS]: "Paquete de horas",
  [TipoDescuento.LIGA_REGULAR]: "Liga regular",
  [TipoDescuento.ESCUELA_AFILIADA]: "Escuela afiliada",
};

/** Cómo se nombra un descuento en pantalla: el tipo y cuánto bonifica. */
export function etiquetaDescuento(descuento) {
  const tipo = ETIQUETA_TIPO_DESCUENTO[descuento.tipo] ?? descuento.tipo ?? "Descuento";
  if (descuento.porcentaje != null) return `${tipo} (${Number(descuento.porcentaje)}%)`;
  if (descuento.montoFijo != null) return `${tipo} (${pesos(descuento.montoFijo)})`;
  return tipo;
}

/**
 * Cuánto bonifica un descuento sobre un monto.
 *
 * Un descuento es por porcentaje o por monto fijo, nunca las dos cosas; el
 * monto fijo nunca puede bonificar más que el total, porque no existe un
 * cobro negativo.
 */
export function bonificacionDe(descuento, base) {
  const monto = Number(base) || 0;
  if (descuento.porcentaje != null) {
    return Math.round(monto * (Number(descuento.porcentaje) / 100) * 100) / 100;
  }
  if (descuento.montoFijo != null) {
    return Math.min(Number(descuento.montoFijo), monto);
  }
  return 0;
}

/**
 * Desglose del cobro: original, lo bonificado y el total.
 *
 * Es lo que pide el criterio de aceptación del ticket. Los descuentos se
 * aplican en cadena sobre el monto original, no uno sobre el resultado del
 * anterior, para que el orden en que se eligen no cambie el total.
 */
export function desglosar(montoOriginal, descuentos = []) {
  const original = Number(montoOriginal) || 0;
  const aplicados = descuentos.map((d) => ({ descuento: d, bonificacion: bonificacionDe(d, original) }));
  const bonificado = Math.min(
    original,
    Math.round(aplicados.reduce((acc, a) => acc + a.bonificacion, 0) * 100) / 100,
  );

  return {
    original,
    aplicados,
    bonificado,
    total: Math.round((original - bonificado) * 100) / 100,
    hayDescuento: bonificado > 0,
  };
}

/** Desglose de un cobro ya registrado: el backend guarda el monto base y el final, no cada descuento aplicado. */
export function desgloseDeCobro(cobro) {
  const total = Number(cobro.montoFinal) || 0;
  const original = Number(cobro.montoBase ?? cobro.montoFinal) || 0;
  const bonificado = Math.round((original - total) * 100) / 100;
  const hayDescuento = bonificado > 0;

  return {
    original,
    aplicados: hayDescuento ? [{ descuento: { id: "descuentos", tipo: "Descuentos" }, bonificacion: bonificado }] : [],
    bonificado,
    total,
    hayDescuento,
  };
}

/**
 * Validación del formulario de alta de cobro (CU51: cobrar la seña de una reserva).
 *
 * El monto y los descuentos los calcula siempre el backend, así que acá no
 * se piden: solo lo que viaja en CobroRequest. numeroOperacion es
 * obligatorio para débito y crédito (lo da la terminal física); cuotas solo
 * aplica a crédito, y si no se indica el backend cobra en 1 cuota.
 */
export function validarCobro(datos) {
  const errores = {};

  if (!datos.reservaId) errores.reservaId = "Elegí la reserva.";
  if (!datos.medioPago) errores.medioPago = "Elegí el medio de pago.";

  const pasaPorTerminal = datos.medioPago === MedioPago.DEBITO || datos.medioPago === MedioPago.CREDITO;
  const numeroOperacion = String(datos.numeroOperacion ?? "").trim();
  if (pasaPorTerminal && !numeroOperacion) {
    errores.numeroOperacion = "Indicá el número de operación que dio la terminal.";
  } else if (pasaPorTerminal && !/^\d{1,20}$/.test(numeroOperacion)) {
    errores.numeroOperacion = "El número de operación tiene que tener solo números, hasta 20 dígitos.";
  }

  if (datos.medioPago === MedioPago.CREDITO && datos.cuotas !== "" && datos.cuotas != null) {
    const cuotas = Number(datos.cuotas);
    if (!Number.isInteger(cuotas) || cuotas < 1 || cuotas > 24) {
      errores.cuotas = "Las cuotas tienen que estar entre 1 y 24.";
    }
  }

  return errores;
}

/** Las fechas viajan como yyyy-mm-dd, así que se pueden comparar como texto. Sin alguna de las dos, no hay rango que validar. */
export function rangoDeFechasValido(desde, hasta) {
  return !desde || !hasta || desde <= hasta;
}

/** Resumen de las tarjetas del listado: solo suma lo pagado. `totalElementos` viene del backend. */
export function resumirCobros(cobros, totalElementos = cobros.length) {
  const pagados = cobros.filter((c) => c.estado === EstadoCobro.PAGADO);
  const ingresos = pagados.filter((c) => !esReembolso(c)).reduce((acc, c) => acc + Number(c.montoFinal ?? 0), 0);
  const reembolsos = pagados.filter(esReembolso).reduce((acc, c) => acc + Number(c.montoFinal ?? 0), 0);

  return {
    total: totalElementos,
    ingresos,
    reembolsos,
    neto: Math.round((ingresos - reembolsos) * 100) / 100,
  };
}
