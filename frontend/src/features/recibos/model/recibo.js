// Modelo del recibo, alineado con la entidad del backend: un recibo por cobro, con su número correlativo, su monto y su fecha de emisión.
import { ETIQUETA_CONCEPTO, ETIQUETA_MEDIO } from "@/features/cobros/model/cobro";

export { pesos, fechaHora, fechaCorta } from "@/features/cobros/model/cobro";

/** El número que identifica al recibo. Es único y lo asigna el backend. */
export function numero(recibo) {
  return recibo.numeroRecibo ?? `#${recibo.id ?? ""}`;
}

/** Concepto legible: el propio del recibo, o el del cobro que lo originó. */
export function conceptoLegible(recibo) {
  const concepto = recibo.concepto ?? recibo.cobro?.concepto;
  return concepto ? (ETIQUETA_CONCEPTO[concepto] ?? concepto) : "—";
}

/** Medio de pago con el que se cobró: el propio del recibo, o el del cobro que lo originó. */
export function medioLegible(recibo) {
  const medio = recibo.medioPago ?? recibo.cobro?.medioPago;
  return medio ? (ETIQUETA_MEDIO[medio] ?? medio) : "—";
}

export const ADVERTENCIA_ANULACION =
  "Al anularlo se invalida el comprobante que ya se le entregó a la persona. Si el recibo pertenece a un período fiscal cerrado, el sistema bloquea la anulación por auditoría.";

/** Un recibo anulado no vuelve a estar vigente ni se edita. */
export function estaAnulado(recibo) {
  return recibo.estado === "ANULADO";
}

/** Un recibo anulado ya no admite más acciones (anular, etc.). */
export function esEditable(recibo) {
  return !estaAnulado(recibo);
}

/** Resumen de las tarjetas del listado. `totalElementos` viene del backend. */
export function resumirRecibos(recibos, totalElementos = recibos.length) {
  const vigentes = recibos.filter((r) => !estaAnulado(r));
  return {
    total: totalElementos,
    vigentes: vigentes.length,
    anulados: recibos.length - vigentes.length,
    monto: Math.round(vigentes.reduce((acc, r) => acc + Number(r.monto ?? 0), 0) * 100) / 100,
  };
}
