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
import { eliminarCategoriaInsumo } from "../api/insumosApi";
import { mensajeDeError } from "../api/errores";

/**
 * Baja de una categoría de insumo. El backend la rechaza (409) si hay
 * insumos —activos o dados de baja— que todavía la tienen asignada; ese
 * mensaje ya viene explicando cuántos son, así que se muestra tal cual.
 */
export function EliminarCategoriaDialog({ abierto, categoria, onCerrar, onEliminada }) {
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!categoria) return null;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarCategoriaInsumo(categoria.id);
      onEliminada(`Se eliminó la categoría ${categoria.nombre}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar la categoría."));
    } finally {
      setBorrando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Eliminar categoría</DialogTitle>
          <DialogDescription>
            Vas a eliminar <strong className="text-foreground">{categoria.nombre}</strong>. Esta acción no se puede deshacer.
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
            {borrando ? "Eliminando..." : "Eliminar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
