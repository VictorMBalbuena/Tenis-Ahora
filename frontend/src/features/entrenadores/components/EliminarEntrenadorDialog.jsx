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
import { ETIQUETA_VALIDACION, dniFormateado, nombreCompleto } from "../model/entrenador";
import { eliminarEntrenador } from "../api/entrenadoresApi";
import { mensajeDeError } from "@/lib/erroresApi";

// Baja de un entrenador (CU28); el backend la rechaza si tiene historial asociado (entrenamientos, etc.).
export function EliminarEntrenadorDialog({ abierto, entrenador, onCerrar, onEliminado }) {
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!entrenador) return null;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarEntrenador(entrenador.id);
      onEliminado(`Se eliminó a ${nombreCompleto(entrenador)}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el entrenador."));
    } finally {
      setBorrando(false);
    }
  }

  const resumen = [
    ["Nombre", nombreCompleto(entrenador)],
    ["DNI", dniFormateado(entrenador.dni)],
    ["Correo", entrenador.email],
    ["Certificación", ETIQUETA_VALIDACION[entrenador.estadoValidacion]],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar entrenador</DialogTitle>
          <DialogDescription>
            Se elimina del sistema junto con su certificación deportiva registrada. Esta acción no se puede deshacer.
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
          <Button variant="destructive" disabled={borrando} onClick={confirmar}>
            {borrando ? "Eliminando..." : "Eliminar entrenador"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
