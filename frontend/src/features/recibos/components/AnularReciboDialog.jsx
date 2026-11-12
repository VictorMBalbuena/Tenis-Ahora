import { useState } from "react";
import { AlertCircle, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ADVERTENCIA_ANULACION, conceptoLegible, fechaHora, numero, pesos } from "../model/recibo";
import { anularRecibo } from "../api/recibosApi";
import { mensajeDeError } from "@/lib/erroresApi";

/**
 * Anulación de un recibo (CU60).
 *
 * El caso de uso pide avisar que se invalida el comprobante entregado y
 * confirmar antes de hacerlo; si el recibo es de un período fiscal cerrado,
 * el backend bloquea la operación por auditoría.
 */
export function AnularReciboDialog({ abierto, recibo, onCerrar, onAnulado }) {
  const [error, setError] = useState("");
  const [anulando, setAnulando] = useState(false);

  if (!recibo) return null;

  async function confirmar() {
    setError("");
    setAnulando(true);
    try {
      await anularRecibo(recibo.id);
      onAnulado(`Se anuló el recibo ${numero(recibo)}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo anular el recibo."));
    } finally {
      setAnulando(false);
    }
  }

  const resumen = [
    ["N° de recibo", numero(recibo)],
    ["Concepto", conceptoLegible(recibo)],
    ["Monto", pesos(recibo.monto)],
    ["Emitido el", fechaHora(recibo.fechaEmision)],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Anular recibo</DialogTitle>
          <DialogDescription>El cobro queda registrado; lo que se invalida es el comprobante.</DialogDescription>
        </DialogHeader>

        <dl className="divide-y divide-border rounded-xl border border-border text-sm">
          {resumen.map(([etiqueta, valor]) => (
            <div key={etiqueta} className="flex items-center justify-between gap-3 px-3.5 py-2.5">
              <dt className="text-muted-foreground">{etiqueta}</dt>
              <dd className="min-w-0 truncate text-right font-medium tabular-nums">{valor}</dd>
            </div>
          ))}
        </dl>

        <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-aviso-bg)] p-3.5 text-sm text-[var(--estado-aviso-fg)]">
          <TriangleAlert className="mt-0.5 size-4 shrink-0" />
          <span>{ADVERTENCIA_ANULACION}</span>
        </p>

        {error && (
          <p role="alert" className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            {error}
          </p>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button variant="destructive" disabled={anulando} onClick={confirmar}>
            {anulando ? "Anulando..." : "Anular recibo"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
