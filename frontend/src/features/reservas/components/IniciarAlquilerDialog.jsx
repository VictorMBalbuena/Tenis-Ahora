import { useState } from "react";
import { Play, Loader2, AlertCircle, Package } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { iniciarAlquiler } from "../api/reservasApi";
import { formatearHorarioTurno, extraerMensajeError } from "../model/reserva";

export function IniciarAlquilerDialog({ open, reserva, onOpenChange, onIniciado }) {
  const [iniciando, setIniciando] = useState(false);
  const [error, setError] = useState(null);

  if (!reserva) return null;

  const numero = reserva.numero || reserva.codigo || `R-${String(reserva.id).padStart(6, "0")}`;
  const nombreCancha =
    reserva.cancha || reserva.canchaNombre || (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
  const horario = formatearHorarioTurno(reserva.horaInicio, reserva.horaFin);
  const insumos = Array.isArray(reserva.insumos) ? reserva.insumos : [];

  const handleIniciar = async () => {
    try {
      setIniciando(true);
      setError(null);
      const data = await iniciarAlquiler(reserva.id);
      if (onIniciado) {
        onIniciado(data);
      }
      onOpenChange(false);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo iniciar el alquiler de la cancha."));
    } finally {
      setIniciando(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(val) => !iniciando && onOpenChange(val)}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
              <Play className="h-4 w-4 fill-current ml-0.5" />
            </span>
            <span>Iniciar alquiler de cancha</span>
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
            Al registrar el ingreso de los jugadores, la reserva pasará a estado{" "}
            <strong className="text-emerald-600 dark:text-emerald-400">EN CURSO</strong>.
          </p>

          {insumos.length > 0 ? (
            <div className="rounded-xl border border-border bg-muted/40 p-3.5 space-y-2">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <Package className="h-4 w-4" />
                <span>Insumos a entregar a los jugadores:</span>
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
                Estos insumos se descontarán del depósito y figurarán en uso hasta el cierre del turno.
              </p>
            </div>
          ) : (
            <div className="rounded-xl border border-border bg-muted/30 p-3 text-xs text-muted-foreground">
              Esta reserva no solicitó insumos adicionales. Solo se habilita la cancha.
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            disabled={iniciando}
            onClick={() => onOpenChange(false)}
            className="rounded-full"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={iniciando}
            onClick={handleIniciar}
            className="rounded-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold"
          >
            {iniciando ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Iniciando...
              </>
            ) : (
              <>
                <Play className="mr-2 h-4 w-4 fill-current" />
                Confirmar inicio
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

