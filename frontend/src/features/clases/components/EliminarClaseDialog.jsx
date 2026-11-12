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
import { ETIQUETA_NIVEL, horario, nombreCancha, nombreProfesor, vigencia } from "../model/clase";
import { eliminarClase } from "../api/clasesApi";
import { mensajeDeError } from "@/lib/erroresApi";

// Baja de una clase (CU35). Si ya tiene alumnos inscriptos conviene avisar antes: borrarla los deja sin la actividad.
export function EliminarClaseDialog({ abierto, clase, onCerrar, onEliminado }) {
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!clase) return null;

  const tieneInscriptos = (clase.cupoOcupado ?? 0) > 0;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarClase(clase.id);
      onEliminado(`Se eliminó la clase "${clase.nombre}".`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar la clase."));
    } finally {
      setBorrando(false);
    }
  }

  const resumen = [
    ["Nombre", clase.nombre],
    ["Vigencia", vigencia(clase)],
    ["Horario", horario(clase)],
    ["Profesor", nombreProfesor(clase)],
    ["Cancha", nombreCancha(clase)],
    ["Nivel", ETIQUETA_NIVEL[clase.nivel] ?? clase.nivel],
    ["Inscriptos", `${clase.cupoOcupado ?? 0} de ${clase.cupoMaximo}`],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar clase</DialogTitle>
          <DialogDescription>
            Se elimina la clase de la agenda. Esta acción no se puede deshacer.
          </DialogDescription>
        </DialogHeader>

        <dl className="divide-y divide-border rounded-xl border border-border text-sm">
          {resumen.map(([clave, valor]) => (
            <div key={clave} className="flex items-center justify-between gap-3 px-3.5 py-2.5">
              <dt className="text-muted-foreground">{clave}</dt>
              <dd className="font-medium tabular-nums">{valor}</dd>
            </div>
          ))}
        </dl>

        {tieneInscriptos && (
          <p className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>
              Hay {clase.cupoOcupado} {clase.cupoOcupado === 1 ? "alumno inscripto" : "alumnos inscriptos"}. Si la
              eliminás, se quedan sin la actividad: conviene avisarles o reprogramarla.
            </span>
          </p>
        )}

        {error && (
          <p
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            {error}
          </p>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button variant="destructive" disabled={borrando} onClick={confirmar}>
            {borrando ? "Eliminando..." : "Eliminar clase"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
