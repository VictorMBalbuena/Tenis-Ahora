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
import { ETIQUETA_MODALIDAD, Modalidad } from "../model/clase";

/**
 * Asignar o cambiar el instructor de una clase particular (CU44).
 *
 * Solo se ofrecen profesores con la certificación validada y libres en el
 * horario de la clase (RD02, RD03): los calcula el backend, no hay que
 * filtrarlos acá. Es una acción aparte de la modificación completa porque en
 * una clase particular el instructor se reasigna seguido, sin tocar el resto
 * de los datos.
 */
export function AsignarInstructorDialog({ abierto, clase, onCerrar, onAsignado }) {
  const [instructores, setInstructores] = useState([]);
  const [profesorId, setProfesorId] = useState("");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!abierto || !clase) return;

    let vigente = true;
    listarInstructoresDisponibles(clase.id)
      .then((data) => vigente && setInstructores(data || []))
      .catch(() => vigente && setInstructores([]));

    return () => {
      vigente = false;
    };
  }, [abierto, clase]);

  if (!clase) return null;

  const esParticular = clase.modalidad === Modalidad.PARTICULAR;
  const actual = clase.profesorId != null ? String(clase.profesorId) : "";

  const opciones = instructores.map((i) => ({ value: String(i.id), label: i.nombre }));

  async function asignar() {
    if (!profesorId) return;
    setError("");
    setGuardando(true);
    try {
      await asignarInstructor(clase.id, { rol: "PROFESOR", instructorId: Number(profesorId) });
      const elegido = instructores.find((i) => String(i.id) === profesorId);
      onAsignado(`Se asignó a ${elegido ? elegido.nombre : "el instructor"} a la clase.`);
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
            Clase {ETIQUETA_MODALIDAD[clase.modalidad]?.toLowerCase() ?? ""}. Hoy la dicta {clase.profesor ?? "—"}.
          </DialogDescription>
        </DialogHeader>

        {!esParticular && (
          <p className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]">
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>
              Esta clase es grupal. Para cambiarle el profesor conviene usar la modificación completa, así
              queda registrado junto con el resto de los datos.
            </span>
          </p>
        )}

        <CampoFormulario
          id="asignar-instructor"
          etiqueta="Instructor"
          info="Solo aparecen los profesores con la certificación validada y libres en ese horario."
        >
          {() => (
            <SelectField
              name="profesorId"
              value={profesorId}
              onChange={(e) => setProfesorId(e.target.value)}
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
          <Button disabled={!profesorId || profesorId === actual || guardando} onClick={asignar}>
            <UserCog />
            {guardando ? "Asignando..." : "Asignar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
