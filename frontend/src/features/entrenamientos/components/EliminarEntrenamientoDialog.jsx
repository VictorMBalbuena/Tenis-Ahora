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
import { fechaCorta, horario, nombreCancha, nombreEntrenador } from "../model/entrenamiento";
import { eliminarEntrenamiento } from "../api/entrenamientosApi";
import { mensajeDeError } from "@/lib/erroresApi";

// Baja de un entrenamiento (CU40). Si ya tiene alumnos inscriptos conviene avisar antes.
export function EliminarEntrenamientoDialog({ abierto, entrenamiento, onCerrar, onEliminado }) {
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!entrenamiento) return null;

  const tieneInscriptos = (entrenamiento.inscriptos ?? 0) > 0;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarEntrenamiento(entrenamiento.id);
      onEliminado(`Se eliminó el entrenamiento del ${fechaCorta(entrenamiento.fecha)}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el entrenamiento."));
    } finally {
      setBorrando(false);
    }
  }

  const resumen = [
    ["Fecha", fechaCorta(entrenamiento.fecha)],
    ["Horario", horario(entrenamiento)],
    ["Entrenador", nombreEntrenador(entrenamiento)],
    ["Cancha", nombreCancha(entrenamiento)],
    ["Objetivo", entrenamiento.objetivo || "—"],
    ["Inscriptos", `${entrenamiento.inscriptos ?? 0} de ${entrenamiento.cupoMaximo}`],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar entrenamiento</DialogTitle>
          <DialogDescription>
            Se elimina el entrenamiento de la agenda. Esta acción no se puede deshacer.
          </DialogDescription>
        </DialogHeader>

        <dl className="divide-y divide-border rounded-xl border border-border text-sm">
          {resumen.map(([clave, valor]) => (
            <div key={clave} className="flex items-center justify-between gap-3 px-3.5 py-2.5">
              <dt className="text-muted-foreground">{clave}</dt>
              <dd className="min-w-0 truncate text-right font-medium">{valor}</dd>
            </div>
          ))}
        </dl>

        {tieneInscriptos && (
          <p className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>
              Hay {entrenamiento.inscriptos}{" "}
              {entrenamiento.inscriptos === 1 ? "alumno inscripto" : "alumnos inscriptos"}. Si lo eliminás, se
              quedan sin la actividad.
            </span>
          </p>
        )}

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
            {borrando ? "Eliminando..." : "Eliminar entrenamiento"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
