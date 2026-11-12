import { useState } from "react";
import { AlertCircle, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CampoFormulario } from "@/components/ui/campo-formulario";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { mensajeDeError } from "@/lib/erroresApi";
import { ConceptoCobro, ETIQUETA_CONCEPTO, ETIQUETA_MEDIO, EstadoCobro, MedioPago, descripcionOrigen, fechaHora, pesos } from "../model/cobro";
import { anularCobro } from "../api/cobrosApi";

// Anular un cobro (CU55): pide el motivo y el cobro queda en el historial como anulado.
export function AnularCobroDialog({ abierto, cobro, onCerrar, onAnulado }) {
  const [motivo, setMotivo] = useState("");
  const [errorMotivo, setErrorMotivo] = useState("");
  const [error, setError] = useState("");
  const [anulando, setAnulando] = useState(false);

  if (!cobro) return null;

  async function confirmar() {
    setError("");
    if (!motivo.trim()) {
      setErrorMotivo("Indicá el motivo.");
      return;
    }
    setErrorMotivo("");
    setAnulando(true);
    try {
      await anularCobro(cobro.id, motivo.trim());
      onAnulado(`Se anuló el cobro de ${pesos(cobro.montoFinal)}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo anular el cobro."));
    } finally {
      setAnulando(false);
    }
  }

  const estabaPagado = cobro.estado === EstadoCobro.PAGADO;
  const resumen = [
    ["Se cobró por", descripcionOrigen(cobro)],
    ["Concepto", ETIQUETA_CONCEPTO[cobro.concepto] ?? cobro.concepto],
    ["Medio de pago", ETIQUETA_MEDIO[cobro.medioPago] ?? cobro.medioPago],
    ["Monto", pesos(cobro.montoFinal)],
    ["Fecha", fechaHora(cobro.fecha)],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Anular cobro</DialogTitle>
          <DialogDescription>
            {estabaPagado && (cobro.concepto === ConceptoCobro.SENA || cobro.concepto === ConceptoCobro.INSCRIPCION)
              ?"La reserva o la inscripción vuelve a quedar pendiente de pago. Esta acción no se puede deshacer."
              : "Esta acción no se puede deshacer."}
          </DialogDescription>
        </DialogHeader>

        <dl className="divide-y divide-border rounded-xl border border-border text-sm">
          {resumen.map(([etiqueta, valor]) => (
            <div key={etiqueta} className="flex items-center justify-between gap-3 px-3.5 py-2.5">
              <dt className="text-muted-foreground">{etiqueta}</dt>
              <dd className="min-w-0 truncate text-right font-medium tabular-nums">{valor}</dd>
            </div>
          ))}
        </dl>

        {estabaPagado && cobro.medioPago === MedioPago.QR && (
          <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-aviso-bg)] p-3.5 text-sm text-[var(--estado-aviso-fg)]">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>La devolución del dinero se hace desde el panel de Mercado Pago.</span>
          </p>
        )}

        <CampoFormulario id="anulacion-motivo" etiqueta="Motivo" error={errorMotivo}>
          {(props) => (
            <Textarea
              {...props}
              value={motivo}
              maxLength={255}
              placeholder="Se cargó a la reserva equivocada"
              onChange={(e) => setMotivo(e.target.value)}
            />
          )}
        </CampoFormulario>

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
            {anulando ? "Anulando..." : "Anular cobro"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
