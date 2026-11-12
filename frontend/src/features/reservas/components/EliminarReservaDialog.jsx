import { useState } from "react";
import { AlertTriangle, Loader2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { formatearMoneda, extraerMensajeError } from "../model/reserva";

function formatearFechaCorta(fechaStr) {
  if (!fechaStr) return "";
  const partes = fechaStr.split("-");
  if (partes.length === 3) {
    return `${partes[2]}/${partes[1]}`;
  }
  return fechaStr;
}

export function EliminarReservaDialog({
  open,
  reserva,
  onOpenChange,
  onConfirm,
}) {
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState(null);

  if (!reserva) return null;

  const titular = reserva.socioTitular
    ? `${reserva.socioTitular.nombre || ""} ${reserva.socioTitular.apellido || ""}`.trim()
    : reserva.usuarioNombreCompleto || reserva.titular || "—";

  const fechaCorta = formatearFechaCorta(reserva.fecha);
  const turnoTexto = `${fechaCorta} ${reserva.horaInicio || ""}`.trim();

  const handleEliminar = async () => {
    try {
      setEliminando(true);
      setError(null);
      if (onConfirm) {
        await onConfirm(reserva);
      }
      onOpenChange(false);
    } catch (err) {
      // `err.message` es el texto de axios ("Request failed with status code 422"), que no le dice
      // nada a nadie. El motivo real viene en la respuesta de la API.
      setError(extraerMensajeError(err, "No es posible cancelar esta reserva."));
    } finally {
      setEliminando(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md p-0 overflow-hidden rounded-xl border border-border bg-card">
        <DialogHeader className="px-6 pt-6 pb-2">
          <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-danger-bg text-danger-fg">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <DialogTitle className="text-lg font-bold tracking-tight text-foreground">
            ¿Eliminar reserva?
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 px-6 pb-4">
          {error && (
            <div className="rounded-lg bg-danger-bg/20 p-3 text-xs text-danger-fg border border-danger-bg/30">
              {error}
            </div>
          )}

          {/* Caja Resumen */}
          <div className="rounded-xl bg-muted p-4">
            <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
              <div>
                <dt className="text-xs text-muted-foreground">Reserva</dt>
                <dd className="mt-0.5 text-sm font-semibold text-foreground">{reserva.codigo}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Usuario</dt>
                <dd className="mt-0.5 text-sm font-semibold text-foreground">{titular}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Turno</dt>
                <dd className="mt-0.5 text-sm font-medium text-foreground">{turnoTexto}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Seña abonada</dt>
                <dd className="mt-0.5 text-sm font-medium text-foreground">
                  {formatearMoneda(reserva.montoSenia || 0)}
                </dd>
              </div>
            </dl>
          </div>

          {/* Banner Aviso */}
          <div className="flex items-center gap-3 rounded-xl bg-warning-bg px-4 py-3 text-warning-fg">
            <AlertTriangle className="h-5 w-5 shrink-0" />
            <p className="text-xs font-semibold">
              Cancelación dentro del plazo
            </p>
          </div>
        </div>

        <DialogFooter className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 px-6 pb-6 pt-2 bg-card">
          <Button
            type="button"
            variant="ghost"
            className="rounded-full"
            onClick={() => onOpenChange(false)}
            disabled={eliminando}
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="destructive"
            className="rounded-full"
            onClick={handleEliminar}
            disabled={eliminando}
          >
            {eliminando ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
            <span>Eliminar</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default EliminarReservaDialog;
