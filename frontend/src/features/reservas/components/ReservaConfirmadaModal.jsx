import { Clock, CreditCard, CalendarCheck, ShieldCheck, FileText, Package } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CuentaRegresiva } from "@/components/ui/cuenta-regresiva";
import { ESTADOS_RESERVA, formatearMoneda } from "../model/reserva";

/**
 * Modal de confirmación de reserva exitosa.
 * Sigue el diseño de `reserva-07-confirmada.html`.
 */
export function ReservaConfirmadaModal({
  reserva,
  abierta,
  open,
  onCerrar = () => {},
  onPagar = () => {},
}) {
  const estaAbierta = Boolean(abierta ?? open);

  if (!reserva) return null;

  const numero = reserva.numero || `R-${String(reserva.id || "").padStart(6, "0")}`;
  const estadoInfo = ESTADOS_RESERVA[reserva.estado] || ESTADOS_RESERVA.PENDIENTE_PAGO;

  const total = reserva.total ?? reserva.montoTotal ?? 0;
  const montoSenia = reserva.montoSenia ?? 0;

  return (
    <Dialog
      open={estaAbierta}
      onOpenChange={(v) => {
        if (!v) onCerrar();
      }}
    >
      <DialogContent className="sm:max-w-xl rounded-2xl p-6 sm:p-8 max-h-[90vh] overflow-y-auto">
        <DialogHeader className="items-center text-center space-y-3">
          <span className="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)] shadow-xs">
            <Clock className="h-7 w-7 stroke-[2.5]" />
          </span>

          <div>
            <DialogTitle className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
              Turno retenido
            </DialogTitle>
            <p className="mt-1 text-sm font-mono text-muted-foreground">
              Reserva {numero} {reserva.canchaNombre ? `· ${reserva.canchaNombre}` : ""}
            </p>
          </div>

          <div className="flex flex-wrap justify-center gap-2 pt-1">
            <span className="inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]">
              Falta pagar la seña
            </span>
          </div>
        </DialogHeader>

        {/* 3 Métricas en tarjetas según reserva-07-confirmada.html L202-219 */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 my-4">
          <div className="rounded-xl border border-border bg-card p-3.5 shadow-2xs">
            <span className="flex h-8 w-8 items-center justify-center rounded-md bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
              <ShieldCheck className="h-4 w-4" />
            </span>
            <p className="mt-2.5 text-xs font-semibold text-foreground">Turno guardado</p>
            <p className="mt-0.5 text-[11px] text-muted-foreground">Te lo guardamos mientras pagás</p>
          </div>

          <div className="rounded-xl border border-border bg-card p-3.5 shadow-2xs">
            <span className="flex h-8 w-8 items-center justify-center rounded-md bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
              <FileText className="h-4 w-4" />
            </span>
            <p className="mt-2.5 text-xs font-semibold text-foreground">Recibo al pagar</p>
            <p className="mt-0.5 text-[11px] text-muted-foreground">Se emite con el pago</p>
          </div>

          <div className="rounded-xl border border-border bg-card p-3.5 shadow-2xs">
            <span className="flex h-8 w-8 items-center justify-center rounded-md bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
              <Package className="h-4 w-4" />
            </span>
            <p className="mt-2.5 text-xs font-semibold text-foreground">Insumos apartados</p>
            <p className="mt-0.5 text-[11px] text-muted-foreground">Se descuentan al iniciar</p>
          </div>
        </div>

        {/* Detalle de la reserva en dl según mockup L222-259 */}
        <div className="rounded-xl border border-border bg-card p-4 sm:p-5 shadow-xs">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3.5 text-sm">
            <div>
              <dt className="text-xs text-muted-foreground">Código</dt>
              <dd className="mt-0.5 font-mono font-bold tabular-nums text-foreground">
                {numero}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Estado</dt>
              <dd className="mt-0.5 font-medium text-foreground">
                {estadoInfo.label}
              </dd>
            </div>
            {total > 0 && (
              <div>
                <dt className="text-xs text-muted-foreground">Total del alquiler</dt>
                <dd className="mt-0.5 font-mono tabular-nums font-semibold text-foreground">
                  {formatearMoneda(total)}
                </dd>
              </div>
            )}
            <div>
              <dt className="text-xs text-muted-foreground">Seña a abonar</dt>
              <dd className="mt-0.5 font-mono tabular-nums font-bold text-foreground">
                {formatearMoneda(montoSenia)}
              </dd>
            </div>
          </dl>
        </div>

        {/* Warning banner según mockup L262-265 */}
        <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)] text-xs font-semibold">
          <CreditCard className="h-4 w-4 shrink-0" />
          <p>Pagá la seña para confirmar el turno. Si se termina el tiempo, se libera.</p>
          {reserva.venceEn && <CuentaRegresiva venceEn={reserva.venceEn} className="ml-auto shrink-0 text-sm" />}
        </div>

        {/* Botones de acción según mockup L267-270 */}
        <DialogFooter className="mt-2 flex flex-col sm:flex-row gap-2">
          <button
            type="button"
            onClick={onCerrar}
            className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-colors border border-input bg-background hover:bg-muted h-10 px-5 w-full sm:w-auto cursor-pointer"
          >
            <CalendarCheck className="w-4 h-4 mr-1" />
            <span>Mis Reservas</span>
          </button>
          <button
            type="button"
            onClick={onPagar}
            className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-colors bg-primary text-primary-foreground hover:brightness-95 h-10 px-6 w-full sm:w-auto cursor-pointer shadow-xs"
          >
            <CreditCard className="w-4 h-4 mr-1" />
            <span>Pagar seña</span>
          </button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ReservaConfirmadaModal;
