import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

// Pide el motivo de rechazo (obligatorio) antes de rechazar una certificación (CU31); compartido entre Profesores y Entrenadores.
export function RechazarCertificacionDialog({ abierto, enviando, onCerrar, onConfirmar }) {
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState("");

  function cancelar() {
    setMotivo("");
    setError("");
    onCerrar();
  }

  function confirmar() {
    if (!motivo.trim()) {
      setError("Escribí el motivo del rechazo.");
      return;
    }
    onConfirmar(motivo.trim());
  }

  return (
    <Dialog
      open={abierto}
      onOpenChange={(v) => {
        if (!v) {
          setMotivo("");
          setError("");
          onCerrar();
        }
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Rechazar certificación</DialogTitle>
          <DialogDescription>El motivo queda registrado y se le va a mostrar a la persona.</DialogDescription>
        </DialogHeader>

        <textarea
          value={motivo}
          onChange={(e) => {
            setMotivo(e.target.value);
            setError("");
          }}
          placeholder="Por ejemplo: el archivo del título no es legible"
          rows={3}
          className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
        {error && <p className="text-xs text-destructive">{error}</p>}

        <DialogFooter>
          <Button variant="outline" onClick={cancelar}>
            Cancelar
          </Button>
          <Button variant="destructive" disabled={enviando} onClick={confirmar}>
            {enviando ? "Rechazando..." : "Rechazar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
