import { useState } from "react";
import { CheckCircle2, Loader2, AlertCircle, Package } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { finalizarAlquiler } from "../api/reservasApi";
import { formatearHorarioTurno, extraerMensajeError, formatearMoneda } from "../model/reserva";

export function FinalizarAlquilerDialog({ open, reserva, onOpenChange, onFinalizado }) {
  const [finalizando, setFinalizando] = useState(false);
  const [error, setError] = useState(null);

  if (!reserva) return null;

  const numero = reserva.numero || reserva.codigo || `R-${String(reserva.id).padStart(6, "0")}`;
  const nombreCancha =
    reserva.cancha || reserva.canchaNombre || (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
  const horario = formatearHorarioTurno(reserva.horaInicio, reserva.horaFin);
  const insumos = Array.isArray(reserva.insumos) ? reserva.insumos : [];
  const saldoPendiente = reserva.saldoPendiente ?? ((reserva.montoTotal || 0) - (reserva.montoSenia || 0));

  const handleFinalizar = async () => {
    try {
      setFinalizando(true);
      setError(null);
      const data = await finalizarAlquiler(reserva.id);
      if (onFinalizado) {
        onFinalizado(data);
      }
      onOpenChange(false);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo finalizar el alquiler de la cancha."));
    } finally {
      setFinalizando(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(val) => !finalizando && onOpenChange(val)}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300">
              <CheckCircle2 className="h-4 w-4" />
            </span>
            <span>Finalizar alquiler de cancha</span>
          </DialogTitle>
          <DialogDescription>
            {numero} &mdash; {nombreCancha} ({reserva.fecha} {horario})
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {error && (
            <div className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-sm text-destructive flex items-start gap-2">
              <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <p className="text-sm text-foreground">
            Al registrar la salida, la reserva pasará a estado{" "}
            <strong className="text-blue-600 dark:text-blue-400">FINALIZADA</strong> y la cancha quedará libre.
          </p>

          {insumos.length > 0 && (
            <div className="rounded-xl border border-border bg-muted/40 p-3.5 space-y-2">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <Package className="h-4 w-4" />
                <span>Insumos a reintegrar al depósito:</span>
              </div>
              <ul className="divide-y divide-border/60 text-sm">
                {insumos.map((i, idx) => {
                  const cant = i.cantidad ?? i.cantidadAsignada;
                  return (
                    <li key={i.id || idx} className="flex justify-between py-1.5 font-medium">
                      <span>{i.nombre}</span>
                      <span className="font-mono tabular-nums text-foreground">
                        {cant} {cant === 1 ? "unidad" : "unidades"}
                      </span>
                    </li>
                  );
                })}
              </ul>
              <p className="text-xs text-muted-foreground pt-1">
                Estos insumos volverán a estar inmediatamente disponibles en el stock del club.
              </p>
            </div>
          )}

          {saldoPendiente > 0 && (
            <div className="rounded-xl border border-warning/30 bg-warning/10 p-3 text-xs text-warning-foreground flex items-center justify-between">
              <span>Saldo restante a abonar:</span>
              <span className="font-bold text-sm font-mono">{formatearMoneda(saldoPendiente)}</span>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            disabled={finalizando}
            onClick={() => onOpenChange(false)}
            className="rounded-full"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={finalizando}
            onClick={handleFinalizar}
            className="rounded-full bg-blue-600 hover:bg-blue-700 text-white font-semibold"
          >
            {finalizando ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Finalizando...
              </>
            ) : (
              <>
                <CheckCircle2 className="mr-2 h-4 w-4" />
                Confirmar finalización
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

