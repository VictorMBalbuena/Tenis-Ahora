import { useState } from "react";
import { AlertCircle, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { eliminarTorneo } from "../api/torneosApi";
import { mensajeDeError } from "@/lib/erroresApi";

export function EliminarTorneoDialog({ open, torneo, onClose, onEliminado }) {
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState("");

  if (!torneo) return null;

  async function handleConfirmar() {
    setEliminando(true);
    setError("");
    try {
      await eliminarTorneo(torneo.id);
      onEliminado?.();
      onClose();
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo dar de baja el torneo."));
    } finally {
      setEliminando(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-destructive">
            <Trash2 className="size-5" />
            Dar de baja torneo
          </DialogTitle>
          <DialogDescription>
            ¿Estás seguro de que deseás eliminar el torneo <strong>{torneo.nombre}</strong>?
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="rounded-xl border border-border bg-muted/40 p-3.5 space-y-1.5 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Categoría:</span>
              <span className="font-semibold">{torneo.categoria}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Modalidad:</span>
              <span className="font-semibold">{torneo.modalidad}</span>
            </div>
            {torneo.estado && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Estado:</span>
                <span className="font-semibold">{torneo.estado}</span>
              </div>
            )}
          </div>

          <p className="text-xs text-muted-foreground">
            Esta acción dará de baja el torneo del sistema y no se podrá deshacer.
          </p>

          {error && (
            <p
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {error}
            </p>
          )}
        </div>

        <DialogFooter className="pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled={eliminando}
            onClick={handleConfirmar}
          >
            {eliminando ? "Eliminando..." : "Eliminar torneo"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
