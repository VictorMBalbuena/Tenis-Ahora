import { useState } from "react";
import { AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { eliminarTipoCancha } from "../api/tiposCanchaApi";
import { mensajeDeError } from "@/lib/erroresApi";

export function EliminarTipoCanchaDialog({ abierto, tipoCancha, onCerrar, onEliminado }) {
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!tipoCancha) return null;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarTipoCancha(tipoCancha.id);
      onEliminado(`Se dio de baja el tipo de cancha "${tipoCancha.nombre}".`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el tipo de cancha."));
    } finally {
      setBorrando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Dar de baja tipo de cancha</DialogTitle>
          <DialogDescription>
            Vas a dar de baja el tipo <strong className="text-foreground">{tipoCancha.nombre}</strong>.
            Si tiene canchas activas asociadas, la operación será rechazada.
          </DialogDescription>
        </DialogHeader>

        {error && (
          <p role="alert" className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            {error}
          </p>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onCerrar} disabled={borrando}>
            Cancelar
          </Button>
          <Button variant="destructive" disabled={borrando} onClick={confirmar}>
            {borrando ? "Dando de baja..." : "Dar de baja"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

