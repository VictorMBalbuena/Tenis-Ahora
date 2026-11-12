import { useEffect, useState } from "react";
import { AlertCircle, UserCog } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CampoFormulario } from "@/components/ui/campo-formulario";
import { SelectField } from "@/components/ui/select-field";
import { mensajeDeError } from "@/lib/erroresApi";
import { asignarInstructor, listarInstructoresDisponibles } from "@/features/actividades/api/asignacionInstructorApi";
import { ETIQUETA_MODALIDAD, Modalidad } from "../model/entrenamiento";

/**
 * Asignar o cambiar el entrenador de un entrenamiento particular (CU44).
 *
 * Solo se ofrecen entrenadores con la certificación validada y libres en el
 * horario del entrenamiento (RD02, RD03): los calcula el backend, no hay que
 * filtrarlos acá.
 */
export function AsignarInstructorDialog({ abierto, entrenamiento, onCerrar, onAsignado }) {
  const [instructores, setInstructores] = useState([]);
  const [entrenadorId, setEntrenadorId] = useState("");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!abierto || !entrenamiento) return;

    let vigente = true;
    listarInstructoresDisponibles(entrenamiento.id)
      .then((data) => vigente && setInstructores(data || []))
      .catch(() => vigente && setInstructores([]));

    return () => {
      vigente = false;
    };
  }, [abierto, entrenamiento]);

  if (!entrenamiento) return null;

  const esParticular = entrenamiento.modalidad === Modalidad.PARTICULAR;
  const actual = entrenamiento.entrenadorId != null ? String(entrenamiento.entrenadorId) : "";

  const opciones = instructores.map((i) => ({ value: String(i.id), label: i.nombre }));

  async function asignar() {
    if (!entrenadorId) return;
    setError("");
    setGuardando(true);
    try {
      await asignarInstructor(entrenamiento.id, { rol: "ENTRENADOR", instructorId: Number(entrenadorId) });
      const elegido = instructores.find((i) => String(i.id) === entrenadorId);
      onAsignado(`Se asignó a ${elegido ? elegido.nombre : "el instructor"} al entrenamiento.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo asignar el instructor."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Asignar instructor</DialogTitle>
          <DialogDescription>
            Entrenamiento {ETIQUETA_MODALIDAD[entrenamiento.modalidad]?.toLowerCase() ?? ""}. Hoy lo dirige{" "}
            {entrenamiento.entrenador ?? "—"}.
          </DialogDescription>
        </DialogHeader>

        {!esParticular && (
          <p className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>
              Este entrenamiento es grupal. Para cambiarle el entrenador conviene usar la modificación
              completa, así queda registrado junto con el resto de los datos.
            </span>
          </p>
        )}

        <CampoFormulario
          id="asignar-instructor"
          etiqueta="Instructor"
          info="Solo aparecen los entrenadores con la certificación validada y libres en ese horario."
        >
          {() => (
            <SelectField
              name="entrenadorId"
              value={entrenadorId}
              onChange={(e) => setEntrenadorId(e.target.value)}
              opciones={opciones}
              placeholder="Elegí el instructor"
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
          <Button disabled={!entrenadorId || entrenadorId === actual || guardando} onClick={asignar}>
            <UserCog />
            {guardando ? "Asignando..." : "Asignar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
