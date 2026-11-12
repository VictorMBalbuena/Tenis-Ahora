import { useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Trophy } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { formatearTanteador, validarTanteadorTenis } from "../model/validadorTenis";
import { cargarResultado } from "../api/torneosApi";
import { mensajeDeError } from "@/lib/erroresApi";

export function CargarResultadoDialog({ open, partido, onClose, onGuardado }) {
  const [sets, setSets] = useState([
    { j1: "", j2: "" },
    { j1: "", j2: "" },
    { j1: "", j2: "" },
  ]);
  const [guardando, setGuardando] = useState(false);
  const [errorApi, setErrorApi] = useState("");
  const [prevKey, setPrevKey] = useState(() => `${partido?.id}-${open}`);

  if (prevKey !== `${partido?.id}-${open}`) {
    setPrevKey(`${partido?.id}-${open}`);
    setSets([
      { j1: "", j2: "" },
      { j1: "", j2: "" },
      { j1: "", j2: "" },
    ]);
    setErrorApi("");
  }

  function handleSetChange(setIndex, jugador, valor) {
    const nuevosSets = sets.map((s, idx) => {
      if (idx === setIndex) {
        return { ...s, [jugador]: valor };
      }
      return s;
    });
    setSets(nuevosSets);
    setErrorApi("");
  }

  const { tanteadorString, validacion } = useMemo(() => {
    // Verificar si algún set tiene solo un jugador con valor (incompleto)
    const haySetIncompleto = sets.some(
      (s) => (s.j1 !== "" && s.j2 === "") || (s.j1 === "" && s.j2 !== "")
    );
    if (haySetIncompleto) {
      return {
        tanteadorString: "",
        validacion: {
          valido: false,
          motivo: "Todos los sets iniciados deben incluir los games de ambos jugadores.",
        },
      };
    }

    // Verificar si hay huecos entre sets (ej: Set 1 lleno, Set 2 vacío, Set 3 lleno)
    const completos = sets.filter((s) => s.j1 !== "" && s.j2 !== "");
    const primerVacioIdx = sets.findIndex((s) => s.j1 === "" && s.j2 === "");
    const posteriorLleno =
      primerVacioIdx !== -1 &&
      sets.slice(primerVacioIdx).some((s) => s.j1 !== "" || s.j2 !== "");
    if (posteriorLleno) {
      return {
        tanteadorString: "",
        validacion: {
          valido: false,
          motivo: "No puede haber sets vacíos intermedios.",
        },
      };
    }

    const stringFormateado = formatearTanteador(completos);
    const val = stringFormateado ? validarTanteadorTenis(stringFormateado) : { valido: false };
    return { tanteadorString: stringFormateado, validacion: val };
  }, [sets]);

  if (!partido) return null;

  const nombre1 =
    partido.participanteANombre ||
    partido.participante1?.nombre ||
    partido.participante1?.nombreEquipo ||
    partido.nombreJugador1 ||
    "Participante 1";
  const nombre2 =
    partido.participanteBNombre ||
    partido.participante2?.nombre ||
    partido.participante2?.nombreEquipo ||
    partido.nombreJugador2 ||
    "Participante 2";

  async function handleGuardar() {
    if (!validacion.valido || !tanteadorString) return;

    setGuardando(true);
    setErrorApi("");
    try {
      const res = await cargarResultado(partido.torneoId, partido.id, tanteadorString);
      onGuardado?.(res);
      onClose();
    } catch (err) {
      setErrorApi(mensajeDeError(err, "No se pudo registrar el resultado del partido."));
    } finally {
      setGuardando(false);
    }
  }

  const hayDatosIngresados = sets.some((s) => s.j1 !== "" || s.j2 !== "");

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Trophy className="size-5 text-primary" />
            Cargar resultado del partido
          </DialogTitle>
          <DialogDescription>
            Ingresá los games obtenidos por cada participante según el reglamento oficial (RD14).
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="overflow-hidden rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
                <tr>
                  <th className="py-2.5 px-3 text-left font-semibold">Participante</th>
                  <th className="py-2.5 px-2 text-center font-semibold w-16">Set 1</th>
                  <th className="py-2.5 px-2 text-center font-semibold w-16">Set 2</th>
                  <th className="py-2.5 px-2 text-center font-semibold w-16">Set 3</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                <tr>
                  <td className="py-2.5 px-3 font-medium truncate max-w-[180px]">{nombre1}</td>
                  {[0, 1, 2].map((setIdx) => (
                    <td key={setIdx} className="py-2 px-1 text-center">
                      <input
                        type="number"
                        min="0"
                        max="7"
                        aria-label={`Games de ${nombre1} en el set ${setIdx + 1}`}
                        value={sets[setIdx].j1}
                        onChange={(e) => handleSetChange(setIdx, "j1", e.target.value)}
                        className="h-9 w-full rounded-md border border-input bg-transparent text-center text-sm tabular shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                      />
                    </td>
                  ))}
                </tr>
                <tr>
                  <td className="py-2.5 px-3 font-medium truncate max-w-[180px]">{nombre2}</td>
                  {[0, 1, 2].map((setIdx) => (
                    <td key={setIdx} className="py-2 px-1 text-center">
                      <input
                        type="number"
                        min="0"
                        max="7"
                        aria-label={`Games de ${nombre2} en el set ${setIdx + 1}`}
                        value={sets[setIdx].j2}
                        onChange={(e) => handleSetChange(setIdx, "j2", e.target.value)}
                        className="h-9 w-full rounded-md border border-input bg-transparent text-center text-sm tabular shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                      />
                    </td>
                  ))}
                </tr>
              </tbody>
            </table>
          </div>

          {hayDatosIngresados && validacion.valido && (
            <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--success-bg,#ecfdf5)] text-[var(--success-fg,#065f46)] border border-emerald-200 dark:border-emerald-900">
              <CheckCircle2 className="size-5 shrink-0" />
              <div>
                <p className="text-sm font-semibold">Resultado válido</p>
                <p className="text-xs">
                  Ganador: {validacion.ganador === 1 ? nombre1 : nombre2} ({tanteadorString})
                </p>
              </div>
            </div>
          )}

          {hayDatosIngresados && !validacion.valido && (
            <div className="flex items-start gap-3 rounded-xl px-4 py-3 bg-[var(--danger-bg,#fef2f2)] text-[var(--danger-fg,#991b1b)] border border-red-200 dark:border-red-900">
              <AlertCircle className="size-5 shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-semibold">Resultado inválido</p>
                <p className="text-xs">{validacion.motivo || "Completá al menos 2 sets reglamentarios."}</p>
              </div>
            </div>
          )}

          {errorApi && (
            <p
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {errorApi}
            </p>
          )}
        </div>

        <DialogFooter className="pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={!validacion.valido || guardando}
            onClick={handleGuardar}
          >
            {guardando ? "Guardando..." : "Guardar resultado"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
