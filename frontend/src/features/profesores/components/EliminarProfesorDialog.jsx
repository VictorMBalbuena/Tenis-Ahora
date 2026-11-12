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
import { ETIQUETA_VALIDACION, dniFormateado, nombreCompleto } from "../model/profesor";
import { eliminarProfesor } from "../api/profesoresApi";
import { mensajeDeError } from "@/lib/erroresApi";

// Baja de un profesor (CU23); el backend la rechaza si tiene historial asociado (clases, reservas, etc.).
export function EliminarProfesorDialog({ abierto, profesor, onCerrar, onEliminado }) {
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!profesor) return null;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarProfesor(profesor.id);
      onEliminado(`Se eliminó a ${nombreCompleto(profesor)}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el profesor."));
    } finally {
      setBorrando(false);
    }
  }

  const resumen = [
    ["Nombre", nombreCompleto(profesor)],
    ["DNI", dniFormateado(profesor.dni)],
    ["Correo", profesor.email],
    ["Certificación", ETIQUETA_VALIDACION[profesor.estadoValidacion]],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar profesor</DialogTitle>
          <DialogDescription>
            Se elimina del sistema junto con su título habilitante registrado. Esta acción no se puede deshacer.
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
            {borrando ? "Eliminando..." : "Eliminar profesor"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
