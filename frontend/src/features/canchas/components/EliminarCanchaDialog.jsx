import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import axiosClient from "@/config/axiosClient";
import { etiquetaSuperficie } from "../model/cancha";

/** Baja lógica de una cancha: el backend no borra canchas, las deshabilita (activa: false). */
export function EliminarCanchaDialog({ abierto = false, cancha = null, onCerrar, onEliminada }) {
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState(null);

  const manejarConfirmacion = async () => {
    if (!cancha?.id) return;
    setEliminando(true);
    setError(null);

    try {
      await axiosClient.patch(`/canchas/${cancha.id}`, { activa: false });
      onEliminada(cancha.id);
    } catch (err) {
      setError(err.response?.data?.mensaje || "No se pudo deshabilitar la cancha.");
    } finally {
      setEliminando(false);
    }
  };

  return (
    <Dialog open={abierto} onOpenChange={(ab) => !ab && onCerrar()}>
      <DialogContent className="sm:max-w-md p-6 sm:p-8">
        <DialogHeader className="pb-2">
          <DialogTitle className="text-destructive flex items-center gap-2">
            <AlertTriangle className="size-5 shrink-0" />
            ¿Deshabilitar cancha?
          </DialogTitle>
          <DialogDescription>
            Vas a deshabilitar <strong className="text-foreground">{cancha?.identificador}</strong>
            {cancha?.tipoSuperficie && <> ({etiquetaSuperficie(cancha.tipoSuperficie)})</>}. Deja de aparecer en la grilla de reservas.
          </DialogDescription>
        </DialogHeader>

        {error && (
          <div role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
            {error}
          </div>
        )}

        <DialogFooter className="mt-6 pt-4 border-t border-border gap-2 sm:gap-3">
          <Button type="button" variant="outline" onClick={onCerrar} disabled={eliminando}>
            Cancelar
          </Button>
          <Button type="button" variant="destructive" onClick={manejarConfirmacion} disabled={eliminando}>
            {eliminando ? "Deshabilitando..." : "Deshabilitar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
